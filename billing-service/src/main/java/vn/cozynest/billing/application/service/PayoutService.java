package vn.cozynest.billing.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.dto.PayoutDto;
import vn.cozynest.billing.application.port.BillingIdentityVerificationPort;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.BillingEventOutboxRepository;
import vn.cozynest.billing.domain.repository.MakerCheckerRepository;
import vn.cozynest.billing.domain.repository.PayoutRequestRepository;
import vn.cozynest.billing.domain.repository.RiskProfileRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PayoutService {
    private static final long DEFAULT_MAKER_CHECKER_THRESHOLD = 10_000_000L;

    private final PayoutRequestRepository payoutRepository;
    private final RiskProfileRepository riskProfileRepository;
    private final LedgerService ledgerService;
    private final BillingIdentityVerificationPort identityVerificationPort;
    private final MakerCheckerRepository makerCheckerRepository;
    private final BillingEventOutboxRepository outboxRepository;
    private final String fintechMode;
    private final boolean liveDisbursementEnabled;
    private final long makerCheckerThresholdVnd;

    public PayoutService(PayoutRequestRepository payoutRepository,
                         RiskProfileRepository riskProfileRepository,
                         LedgerService ledgerService) {
        this(payoutRepository, riskProfileRepository, ledgerService, "SANDBOX_ONLY", false);
    }

    public PayoutService(PayoutRequestRepository payoutRepository,
                         RiskProfileRepository riskProfileRepository,
                         LedgerService ledgerService,
                         String fintechMode,
                         boolean liveDisbursementEnabled) {
        this(payoutRepository, riskProfileRepository, ledgerService,
                Optional.empty(), Optional.empty(), Optional.empty(),
                fintechMode, liveDisbursementEnabled, DEFAULT_MAKER_CHECKER_THRESHOLD);
    }

    @Autowired
    public PayoutService(PayoutRequestRepository payoutRepository,
                         RiskProfileRepository riskProfileRepository,
                         LedgerService ledgerService,
                         Optional<BillingIdentityVerificationPort> identityVerificationPort,
                         Optional<MakerCheckerRepository> makerCheckerRepository,
                         Optional<BillingEventOutboxRepository> outboxRepository,
                         @Value("${fintech.mode:SANDBOX_ONLY}") String fintechMode,
                         @Value("${fintech.disbursement.live-enabled:false}") boolean liveDisbursementEnabled,
                         @Value("${fintech.payout.maker-checker-threshold-vnd:10000000}") long makerCheckerThresholdVnd) {
        this.payoutRepository = payoutRepository;
        this.riskProfileRepository = riskProfileRepository;
        this.ledgerService = ledgerService;
        this.identityVerificationPort = identityVerificationPort != null ? identityVerificationPort.orElse(null) : null;
        this.makerCheckerRepository = makerCheckerRepository != null ? makerCheckerRepository.orElse(null) : null;
        this.outboxRepository = outboxRepository != null ? outboxRepository.orElse(null) : null;
        this.fintechMode = fintechMode;
        this.liveDisbursementEnabled = liveDisbursementEnabled;
        this.makerCheckerThresholdVnd = makerCheckerThresholdVnd;
    }

    @Transactional
    public PayoutDto.PayoutResponse requestPayout(String landlordId, PayoutDto.RequestPayoutCommand command) {
        if ("LIVE".equalsIgnoreCase(fintechMode) && !liveDisbursementEnabled) {
            throw new IllegalStateException("BLOCKED_HUMAN_REVIEW [LEGAL-02]: Live disbursement disabled without license/partner sign-off.");
        }
        verifyOtpCode(landlordId, command.otp());

        LocalDate today = LocalDate.now();

        // 1. Enforce FIN-RISK-01 Limits (blacklist, freeze, 24h bank lock, velocity, daily cap)
        RiskProfile riskProfile = riskProfileRepository.findByUserId(landlordId)
                .orElseGet(() -> riskProfileRepository.save(RiskProfile.createDefault(landlordId)));
        riskProfile.validateWithdrawal(command.amount(), today);

        // 2. Validate sufficient wallet balance in Ledger
        String walletAcc = ledgerService.getWalletAccountNumber(landlordId);
        long availableBalance = ledgerService.getAccountBalance(walletAcc);
        if (availableBalance < command.amount()) {
            throw new IllegalStateException("Số dư ví khả dụng không đủ (Hiện có: " + availableBalance + " VND)");
        }

        String payoutCode = "PO-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        boolean needsApproval = command.amount() > makerCheckerThresholdVnd && makerCheckerRepository != null;
        String mcId = null;
        if (needsApproval) {
            MakerCheckerRequest mc = makerCheckerRepository.save(MakerCheckerRequest.create(
                    landlordId,
                    "PAYOUT_APPROVAL",
                    command.amount(),
                    "Rut tien gia tri cao > " + makerCheckerThresholdVnd + " VND",
                    String.format("{\"payoutCode\":\"%s\",\"bankCode\":\"%s\"}", payoutCode, command.bankCode())
            ));
            mcId = mc.getId();
            PayoutRequest pending = PayoutRequest.create(
                    payoutCode, landlordId, command.amount(), command.bankCode(),
                    command.accountNumber(), command.accountHolder(), true, true, mcId
            );
            PayoutRequest saved = payoutRepository.save(pending);
            emitOutbox("PAYOUT_PENDING_APPROVAL", saved);
            return toResponse(saved);
        }

        // 3. Post Idempotent Ledger Entry immediately for auto-approved payouts
        executePayoutLedger(landlordId, landlordId, payoutCode, command.bankCode(), command.accountNumber(), command.amount(), riskProfile, today);

        // 4. Save payout request
        PayoutRequest payout = PayoutRequest.create(
                payoutCode, landlordId, command.amount(), command.bankCode(),
                command.accountNumber(), command.accountHolder(), true, false, null
        );
        payout.complete();
        PayoutRequest saved = payoutRepository.save(payout);
        emitOutbox("PAYOUT_COMPLETED", saved);
        return toResponse(saved);
    }

    @Transactional
    public PayoutDto.PayoutResponse approvePayout(String payoutId, String checkerAdminId) {
        if ("LIVE".equalsIgnoreCase(fintechMode) && !liveDisbursementEnabled) {
            throw new IllegalStateException("BLOCKED_HUMAN_REVIEW [LEGAL-02]: Live disbursement disabled without license/partner sign-off.");
        }
        PayoutRequest payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau rut tien: " + payoutId));
        if ("COMPLETED".equals(payout.getStatus())) {
            return toResponse(payout);
        }
        if (payout.getMakerCheckerRequestId() != null && makerCheckerRepository != null) {
            MakerCheckerRequest mc = makerCheckerRepository.findById(payout.getMakerCheckerRequestId())
                    .orElseThrow(() -> new IllegalStateException("Thieu ban ghi Maker-Checker"));
            mc.approve(checkerAdminId);
            makerCheckerRepository.save(mc);
        } else if (payout.getLandlordId().equals(checkerAdminId)) {
            throw new AccessDeniedException("Khong the tu duyet yeu cau rut tien cua chinh minh");
        }

        LocalDate today = LocalDate.now();
        RiskProfile riskProfile = riskProfileRepository.findByUserId(payout.getLandlordId())
                .orElseGet(() -> riskProfileRepository.save(RiskProfile.createDefault(payout.getLandlordId())));
        riskProfile.validateWithdrawal(payout.getAmount(), today);

        String walletAcc = ledgerService.getWalletAccountNumber(payout.getLandlordId());
        long availableBalance = ledgerService.getAccountBalance(walletAcc);
        if (availableBalance < payout.getAmount()) {
            throw new IllegalStateException("So du vi kha dung khong du de duyet chi");
        }

        executePayoutLedger(payout.getLandlordId(), checkerAdminId, payout.getPayoutCode(),
                payout.getBankCode(), payout.getAccountNumber(), payout.getAmount(), riskProfile, today);
        payout.complete();
        PayoutRequest saved = payoutRepository.save(payout);
        emitOutbox("PAYOUT_COMPLETED", saved);
        return toResponse(saved);
    }

    @Transactional
    public RiskProfile markBankChangedByOwner(String landlordId, String otpCode) {
        verifyOtpCode(landlordId, otpCode);
        RiskProfile profile = riskProfileRepository.findByUserId(landlordId)
                .orElseGet(() -> RiskProfile.createDefault(landlordId));
        profile.markBankChanged(Instant.now());
        RiskProfile saved = riskProfileRepository.save(profile);
        if (outboxRepository != null) {
            String payload = String.format(
                    "{\"landlordId\":\"%s\",\"message\":\"Tai khoan ngan hang da duoc cap nhat, kich hoat khoa rut tien 24h\"}",
                    landlordId
            );
            outboxRepository.save(BillingEventOutbox.create("BANK_ACCOUNT_CHANGED", "RISK_PROFILE", landlordId, payload));
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PayoutDto.PayoutResponse> getLandlordPayouts(String landlordId) {
        return payoutRepository.findByLandlordId(landlordId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void verifyOtpCode(String userId, String otp) {
        if (otp == null || !otp.matches("^[0-9]{6}$")) {
            throw new IllegalArgumentException("Ma OTP xac nhan rut tien phai gom 6 chu so hop le.");
        }
        if (identityVerificationPort != null && !identityVerificationPort.verifyOtp(userId, otp)) {
            throw new IllegalArgumentException("Ma OTP xac thuc khong chinh xac hoac da het han");
        }
    }

    private void executePayoutLedger(String landlordId, String actorId, String payoutCode,
                                     String bankCode, String accountNumber, long amount,
                                     RiskProfile riskProfile, LocalDate today) {
        String walletAcc = ledgerService.getWalletAccountNumber(landlordId);
        ledgerService.postEntryIdempotent(
                "PAYOUT-" + payoutCode,
                "Rút tiền về tài khoản ngân hàng " + bankCode + " " + maskAccount(accountNumber),
                "PAYOUT",
                landlordId,
                actorId,
                List.of(
                        new LedgerDto.PostingItem(walletAcc, PostingDirection.DEBIT, amount),
                        new LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.CREDIT, amount)
                )
        );
        riskProfile.recordWithdrawal(amount, today);
        riskProfileRepository.save(riskProfile);
    }

    private void emitOutbox(String eventType, PayoutRequest payout) {
        if (outboxRepository == null) {
            return;
        }
        String payload = String.format(
                "{\"payoutId\":\"%s\",\"payoutCode\":\"%s\",\"landlordId\":\"%s\",\"amount\":%d,\"status\":\"%s\",\"message\":\"Yeu cau rut tien %s trang thai %s\"}",
                payout.getId(), payout.getPayoutCode(), payout.getLandlordId(),
                payout.getAmount(), payout.getStatus(), payout.getPayoutCode(), payout.getStatus()
        );
        outboxRepository.save(BillingEventOutbox.create(eventType, "PAYOUT", payout.getId(), payload));
    }

    private PayoutDto.PayoutResponse toResponse(PayoutRequest p) {
        return new PayoutDto.PayoutResponse(
                p.getId(), p.getPayoutCode(), p.getAmount(), p.getBankCode(),
                maskAccount(p.getAccountNumber()), p.getAccountHolder(), p.getStatus(), p.getCreatedAt()
        );
    }

    private String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
