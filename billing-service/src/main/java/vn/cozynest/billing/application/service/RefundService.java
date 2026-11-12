package vn.cozynest.billing.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.dto.RefundDto;
import vn.cozynest.billing.application.port.BillingIdentityVerificationPort;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.BillingEventOutboxRepository;
import vn.cozynest.billing.domain.repository.MakerCheckerRepository;
import vn.cozynest.billing.domain.repository.PaymentIntentRepository;
import vn.cozynest.billing.domain.repository.RefundRequestRepository;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefundService {
    private static final long MAKER_CHECKER_THRESHOLD_VND = 5_000_000L;

    private final RefundRequestRepository refundRepository;
    private final MakerCheckerRepository makerCheckerRepository;
    private final LedgerService ledgerService;
    private final BillingIdentityVerificationPort identityVerificationPort;
    private final PaymentIntentRepository paymentIntentRepository;
    private final BillingEventOutboxRepository outboxRepository;
    private final String fintechMode;
    private final boolean liveDisbursementEnabled;

    public RefundService(RefundRequestRepository refundRepository,
                         MakerCheckerRepository makerCheckerRepository,
                         LedgerService ledgerService) {
        this(refundRepository, makerCheckerRepository, ledgerService, "SANDBOX_ONLY", false);
    }

    public RefundService(RefundRequestRepository refundRepository,
                         MakerCheckerRepository makerCheckerRepository,
                         LedgerService ledgerService,
                         String fintechMode,
                         boolean liveDisbursementEnabled) {
        this(refundRepository, makerCheckerRepository, ledgerService,
                Optional.empty(), Optional.empty(), Optional.empty(),
                fintechMode, liveDisbursementEnabled);
    }

    @Autowired
    public RefundService(RefundRequestRepository refundRepository,
                         MakerCheckerRepository makerCheckerRepository,
                         LedgerService ledgerService,
                         Optional<BillingIdentityVerificationPort> identityVerificationPort,
                         Optional<PaymentIntentRepository> paymentIntentRepository,
                         Optional<BillingEventOutboxRepository> outboxRepository,
                         @Value("${fintech.mode:SANDBOX_ONLY}") String fintechMode,
                         @Value("${fintech.disbursement.live-enabled:false}") boolean liveDisbursementEnabled) {
        this.refundRepository = refundRepository;
        this.makerCheckerRepository = makerCheckerRepository;
        this.ledgerService = ledgerService;
        this.identityVerificationPort = identityVerificationPort != null ? identityVerificationPort.orElse(null) : null;
        this.paymentIntentRepository = paymentIntentRepository != null ? paymentIntentRepository.orElse(null) : null;
        this.outboxRepository = outboxRepository != null ? outboxRepository.orElse(null) : null;
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
        if (identityVerificationPort != null && !identityVerificationPort.verifyOtp(tenantId, cmd.otpCode())) {
            throw new IllegalArgumentException("Ma OTP hoan tien khong dung hoac da het han");
        }

        String authoritativeKycName = cmd.expectedKycName();
        if (identityVerificationPort != null) {
            BillingIdentityVerificationPort.KycVerificationSummary kyc = identityVerificationPort.getKycStatus(tenantId);
            if (!kyc.approved() || kyc.verifiedFullName() == null || kyc.verifiedFullName().isBlank()) {
                throw new IllegalArgumentException("Nguoi dung chua hoan tat xac minh KYC de nhan hoan tien");
            }
            authoritativeKycName = kyc.verifiedFullName();
        }
        if (authoritativeKycName == null || authoritativeKycName.isBlank()
                || !isNameMatched(cmd.accountHolder(), authoritativeKycName)) {
            throw new IllegalArgumentException("Ten chu tai khoan ngan hang khong khop ho so KYC da xac minh");
        }

        if (cmd.paymentIntentId() != null && !cmd.paymentIntentId().isBlank() && paymentIntentRepository != null) {
            PaymentIntent intent = paymentIntentRepository.findById(cmd.paymentIntentId())
                    .orElseThrow(() -> new IllegalArgumentException("Khong tim thay giao dich thanh toan goc"));
            if (!intent.getPayerId().equals(tenantId)) {
                throw new AccessDeniedException("Khong co quyen yeu cau hoan tien cho giao dich cua nguoi khac");
            }
            long alreadyRefunded = refundRepository.findByPaymentIntentId(cmd.paymentIntentId()).stream()
                    .filter(r -> !"REJECTED".equals(r.getStatus()) && !"FAILED".equals(r.getStatus()))
                    .mapToLong(RefundRequest::getAmount)
                    .sum();
            if (alreadyRefunded + cmd.amount() > intent.getPaidAmount()) {
                throw new IllegalArgumentException("Tong so tien hoan (" + (alreadyRefunded + cmd.amount())
                        + ") vuot qua so tien da thanh toan (" + intent.getPaidAmount() + ")");
            }
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
        } else {
            emitOutbox("REFUND_PENDING_APPROVAL", refund);
        }
        return toResponse(refundRepository.save(refund));
    }

    @Transactional
    public RefundDto.RefundResponse approveRefund(String refundId, String checkerAdminId) {
        if ("LIVE".equalsIgnoreCase(fintechMode) && !liveDisbursementEnabled) {
            throw new IllegalStateException("BLOCKED_HUMAN_REVIEW [LEGAL-02]: Live refund disbursement disabled without sign-off.");
        }
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

    @Transactional
    public RefundDto.RefundResponse markFailed(String refundId, String reason) {
        RefundRequest refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau hoan tien: " + refundId));
        refund.markFailed(reason);
        RefundRequest saved = refundRepository.save(refund);
        emitOutbox("REFUND_FAILED", saved);
        return toResponse(saved);
    }

    @Transactional
    public RefundDto.RefundResponse retryFailedRefund(String refundId, String adminId) {
        if ("LIVE".equalsIgnoreCase(fintechMode) && !liveDisbursementEnabled) {
            throw new IllegalStateException("BLOCKED_HUMAN_REVIEW [LEGAL-02]: Live refund disbursement disabled without sign-off.");
        }
        RefundRequest refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau hoan tien: " + refundId));
        if (!"FAILED".equals(refund.getStatus())) {
            throw new IllegalStateException("Chi co the thu lai giao dich hoan tien o trang thai FAILED");
        }
        refund.markProcessing();
        executeLedgerRefund(refund, adminId);
        return toResponse(refundRepository.save(refund));
    }

    @Transactional(readOnly = true)
    public List<RefundDto.RefundResponse> getFailedRefunds() {
        return refundRepository.findByStatus("FAILED").stream().map(this::toResponse).toList();
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
        emitOutbox("REFUND_COMPLETED", refund);
    }

    private void emitOutbox(String eventType, RefundRequest refund) {
        if (outboxRepository == null) {
            return;
        }
        String payload = String.format(
                "{\"refundId\":\"%s\",\"refundCode\":\"%s\",\"tenantId\":\"%s\",\"amount\":%d,\"status\":\"%s\",\"message\":\"Hoan tien %s trang thai %s\"}",
                refund.getId(), refund.getRefundCode(), refund.getTenantId(),
                refund.getAmount(), refund.getStatus(), refund.getRefundCode(), refund.getStatus()
        );
        outboxRepository.save(BillingEventOutbox.create(eventType, "REFUND", refund.getId(), payload));
    }

    private boolean isNameMatched(String bankHolder, String expectedKycName) {
        if (expectedKycName == null || expectedKycName.isBlank()) {
            return false;
        }
        return normalizeName(bankHolder).equals(normalizeName(expectedKycName));
    }

    private String normalizeName(String input) {
        String nfd = Normalizer.normalize(input == null ? "" : input.trim(), Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D')
                .replaceAll("\\s+", " ").toUpperCase();
    }

    private RefundDto.RefundResponse toResponse(RefundRequest r) {
        String acc = r.getAccountNumber();
        String maskedAcc = (acc != null && acc.length() > 4) ? "****" + acc.substring(acc.length() - 4) : acc;
        return new RefundDto.RefundResponse(
                r.getId(), r.getRefundCode(), r.getContractId(), r.getTenantId(),
                r.getAmount(), r.getBankCode(), maskedAcc, r.getAccountHolder(),
                r.isOtpVerified(), r.isKycNameMatched(), r.isRequiresMakerChecker(),
                r.getMakerCheckerRequestId(), r.getStatus(), r.getReason(), r.getCreatedAt()
        );
    }
}
