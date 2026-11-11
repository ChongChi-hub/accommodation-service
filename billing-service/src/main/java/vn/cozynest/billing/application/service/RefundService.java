package vn.cozynest.billing.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.dto.RefundDto;
import vn.cozynest.billing.domain.model.MakerCheckerRequest;
import vn.cozynest.billing.domain.model.PostingDirection;
import vn.cozynest.billing.domain.model.RefundRequest;
import vn.cozynest.billing.domain.repository.MakerCheckerRepository;
import vn.cozynest.billing.domain.repository.RefundRequestRepository;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;

@Service
public class RefundService {
    private static final long MAKER_CHECKER_THRESHOLD_VND = 5_000_000L;

    private final RefundRequestRepository refundRepository;
    private final MakerCheckerRepository makerCheckerRepository;
    private final LedgerService ledgerService;
    private final String fintechMode;
    private final boolean liveDisbursementEnabled;

    public RefundService(RefundRequestRepository refundRepository,
                         MakerCheckerRepository makerCheckerRepository,
                         LedgerService ledgerService) {
        this(refundRepository, makerCheckerRepository, ledgerService, "SANDBOX_ONLY", false);
    }

    @Autowired
    public RefundService(RefundRequestRepository refundRepository,
                         MakerCheckerRepository makerCheckerRepository,
                         LedgerService ledgerService,
                         @Value("${fintech.mode:SANDBOX_ONLY}") String fintechMode,
                         @Value("${fintech.disbursement.live-enabled:false}") boolean liveDisbursementEnabled) {
        this.refundRepository = refundRepository;
        this.makerCheckerRepository = makerCheckerRepository;
        this.ledgerService = ledgerService;
        this.fintechMode = fintechMode;
        this.liveDisbursementEnabled = liveDisbursementEnabled;
    }

    @Transactional
    public RefundDto.RefundResponse requestRefund(String tenantId, RefundDto.CreateRefundCommand cmd) {
        if ("LIVE".equalsIgnoreCase(fintechMode) && !liveDisbursementEnabled) {
            throw new IllegalStateException("BLOCKED_HUMAN_REVIEW [LEGAL-02]: Live refund disbursement disabled without sign-off.");
        }
        if (cmd.otpCode() == null || !cmd.otpCode().matches("^[0-9]{6}$")) {
            throw new IllegalArgumentException("Ma OTP hoan tien khong hop le");
        }
        boolean kycMatched = isNameMatched(cmd.accountHolder(), cmd.expectedKycName());
        if (!kycMatched) {
            throw new IllegalArgumentException("Ten chu tai khoan ngan hang khong khop ho so KYC da xac minh");
        }

        boolean needsApproval = cmd.amount() > MAKER_CHECKER_THRESHOLD_VND;
        String refundCode = "RF-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String mcId = null;

        if (needsApproval) {
            MakerCheckerRequest mc = makerCheckerRepository.save(MakerCheckerRequest.create(
                    tenantId, "REFUND_APPROVAL", cmd.amount(),
                    "Hoan tien > 5tr VND: " + cmd.reason(),
                    "{\"refundCode\":\"" + refundCode + "\"}"
            ));
            mcId = mc.getId();
        }

        RefundRequest refund = RefundRequest.create(
                refundCode, cmd.paymentIntentId(), cmd.contractId(), tenantId, cmd.landlordId(),
                cmd.amount(), cmd.bankCode(), cmd.accountNumber(), cmd.accountHolder(),
                true, true, needsApproval, mcId, cmd.reason()
        );
        if (!needsApproval) {
            executeLedgerRefund(refund, tenantId);
        }
        return toResponse(refundRepository.save(refund));
    }

    @Transactional
    public RefundDto.RefundResponse approveRefund(String refundId, String checkerAdminId) {
        RefundRequest refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau hoan tien: " + refundId));
        if ("COMPLETED".equals(refund.getStatus())) {
            return toResponse(refund);
        }
        if (refund.getMakerCheckerRequestId() != null) {
            MakerCheckerRequest mc = makerCheckerRepository.findById(refund.getMakerCheckerRequestId())
                    .orElseThrow(() -> new IllegalStateException("Thieu ban ghi Maker-Checker"));
            mc.approve(checkerAdminId);
            makerCheckerRepository.save(mc);
        }
        executeLedgerRefund(refund, checkerAdminId);
        return toResponse(refundRepository.save(refund));
    }

    @Transactional(readOnly = true)
    public List<RefundDto.RefundResponse> getTenantRefunds(String tenantId) {
        return refundRepository.findByTenantId(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RefundDto.RefundResponse> getAllRefunds() {
        return refundRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RefundDto.RefundResponse getRefundById(String id, String actorId, boolean isAdmin) {
        RefundRequest r = refundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau hoan tien: " + id));
        if (!isAdmin && !r.getTenantId().equals(actorId) && !r.getLandlordId().equals(actorId)) {
            throw new AccessDeniedException("Khong co quyen xem yeu cau hoan tien nay");
        }
        return toResponse(r);
    }

    private void executeLedgerRefund(RefundRequest refund, String actorId) {
        ledgerService.postEntryIdempotent(
                "REFUND-" + refund.getRefundCode(),
                "Hoan tien coc/hop dong " + refund.getContractId() + ": " + refund.getReason(),
                "REFUND", refund.getId(), actorId,
                List.of(
                        new LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.DEBIT, refund.getAmount()),
                        new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.CREDIT, refund.getAmount())
                )
        );
        refund.markCompleted();
    }

    private boolean isNameMatched(String bankHolder, String expectedKycName) {
        if (expectedKycName == null || expectedKycName.isBlank()) {
            return bankHolder != null && !bankHolder.isBlank();
        }
        return normalizeName(bankHolder).equals(normalizeName(expectedKycName));
    }

    private String normalizeName(String input) {
        String nfd = Normalizer.normalize(input == null ? "" : input.trim(), Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D')
                .replaceAll("\\s+", " ").toUpperCase();
    }

    private RefundDto.RefundResponse toResponse(RefundRequest r) {
        return new RefundDto.RefundResponse(
                r.getId(), r.getRefundCode(), r.getContractId(), r.getTenantId(),
                r.getAmount(), r.getBankCode(), r.getAccountNumber(), r.getAccountHolder(),
                r.isOtpVerified(), r.isKycNameMatched(), r.isRequiresMakerChecker(),
                r.getMakerCheckerRequestId(), r.getStatus(), r.getReason(), r.getCreatedAt()
        );
    }
}

