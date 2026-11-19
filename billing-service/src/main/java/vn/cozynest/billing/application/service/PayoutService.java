package vn.cozynest.billing.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.PayoutDto;
import vn.cozynest.billing.application.exception.WebhookSecurityException;
import vn.cozynest.billing.application.port.BillingIdentityVerificationPort;
import vn.cozynest.billing.application.port.BillingNotificationPort;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.*;

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
    private final BankAccountService bankAccountService;
    private final BankTransactionRegistryRepository bankTxRegistry;
    private final BillingNotificationPort notificationPort;
    private final PaymentProofStorageService proofStorageService;
    private final String fintechMode;
    private final boolean liveDisbursementEnabled;
    private final long makerCheckerThresholdVnd;
    private final long withdrawalMinAmount;

    public PayoutService(PayoutRequestRepository payoutRepository, RiskProfileRepository riskProfileRepository, LedgerService ledgerService) {
        this(payoutRepository, riskProfileRepository, ledgerService, "SANDBOX_ONLY", false);
    }
    public PayoutService(PayoutRequestRepository payoutRepository, RiskProfileRepository riskProfileRepository, LedgerService ledgerService, String fintechMode, boolean liveDisbursementEnabled) {
        this(payoutRepository, riskProfileRepository, ledgerService, Optional.empty(), Optional.empty(), Optional.empty(), fintechMode, liveDisbursementEnabled, DEFAULT_MAKER_CHECKER_THRESHOLD);
    }
    public PayoutService(PayoutRequestRepository payoutRepository, RiskProfileRepository riskProfileRepository, LedgerService ledgerService,
                         Optional<BillingIdentityVerificationPort> identityVerificationPort, Optional<MakerCheckerRepository> makerCheckerRepository,
                         Optional<BillingEventOutboxRepository> outboxRepository, String fintechMode, boolean liveDisbursementEnabled, long makerCheckerThresholdVnd) {
        this(payoutRepository, riskProfileRepository, ledgerService, identityVerificationPort, makerCheckerRepository, outboxRepository,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), fintechMode, liveDisbursementEnabled, makerCheckerThresholdVnd, 1000L);
    }
    public PayoutService(PayoutRequestRepository payoutRepository, RiskProfileRepository riskProfileRepository, LedgerService ledgerService,
                         Optional<BillingIdentityVerificationPort> identityVerificationPort, Optional<MakerCheckerRepository> makerCheckerRepository,
                         Optional<BillingEventOutboxRepository> outboxRepository, Optional<BankAccountService> bankAccountService,
                         Optional<BankTransactionRegistryRepository> bankTxRegistry, String fintechMode, boolean liveDisbursementEnabled, long makerCheckerThresholdVnd, long withdrawalMinAmount) {
        this(payoutRepository, riskProfileRepository, ledgerService, identityVerificationPort, makerCheckerRepository, outboxRepository,
                bankAccountService, bankTxRegistry, Optional.empty(), Optional.empty(), fintechMode, liveDisbursementEnabled, makerCheckerThresholdVnd, withdrawalMinAmount);
    }

    @Autowired
    public PayoutService(PayoutRequestRepository payoutRepository, RiskProfileRepository riskProfileRepository, LedgerService ledgerService,
                         Optional<BillingIdentityVerificationPort> identityVerificationPort, Optional<MakerCheckerRepository> makerCheckerRepository,
                         Optional<BillingEventOutboxRepository> outboxRepository, Optional<BankAccountService> bankAccountService,
                         Optional<BankTransactionRegistryRepository> bankTxRegistry, Optional<BillingNotificationPort> notificationPort,
                         Optional<PaymentProofStorageService> proofStorageService,
                         @Value("${fintech.mode:SANDBOX_ONLY}") String fintechMode,
                         @Value("${fintech.disbursement.live-enabled:false}") boolean liveDisbursementEnabled,
                         @Value("${fintech.payout.maker-checker-threshold-vnd:10000000}") long makerCheckerThresholdVnd,
                         @Value("${fintech.withdrawal.min-amount:${withdrawal.min.amount:1000}}") long withdrawalMinAmount) {
        this.payoutRepository = payoutRepository;
        this.riskProfileRepository = riskProfileRepository;
        this.ledgerService = ledgerService;
        this.identityVerificationPort = identityVerificationPort != null ? identityVerificationPort.orElse(null) : null;
        this.makerCheckerRepository = makerCheckerRepository != null ? makerCheckerRepository.orElse(null) : null;
        this.outboxRepository = outboxRepository != null ? outboxRepository.orElse(null) : null;
        this.bankAccountService = bankAccountService != null ? bankAccountService.orElse(null) : null;
        this.bankTxRegistry = bankTxRegistry != null ? bankTxRegistry.orElse(null) : null;
        this.notificationPort = notificationPort != null ? notificationPort.orElse(null) : null;
        this.proofStorageService = proofStorageService != null ? proofStorageService.orElse(null) : null;
        this.fintechMode = fintechMode;
        this.liveDisbursementEnabled = liveDisbursementEnabled;
        this.makerCheckerThresholdVnd = makerCheckerThresholdVnd;
        this.withdrawalMinAmount = Math.max(1000L, withdrawalMinAmount);
    }

    @Transactional(rollbackFor = Exception.class)
    public PayoutDto.PayoutResponse requestPayout(String landlordId, PayoutDto.RequestPayoutCommand command) {
        return requestPayout(landlordId, command, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public PayoutDto.PayoutResponse requestPayout(String landlordId, PayoutDto.RequestPayoutCommand command, String idempotencyKey) {
        assertDisbursementAllowed();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<PayoutRequest> existing = payoutRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) return PayoutLedgerHelper.toResponse(existing.get());
        }
        verifyOtpCode(landlordId, command.otp());
        if (command.amount() < withdrawalMinAmount) throw new IllegalArgumentException("Số tiền rút tối thiểu theo cấu hình là " + withdrawalMinAmount + " VND");
        String bankCode = command.bankCode().trim().toUpperCase();
        String accountNumber = command.accountNumber().trim();
        String accountHolder = command.accountHolder().trim().toUpperCase();
        if (bankAccountService != null) {
            UserBankAccount verified = bankAccountService.findDomainByUserId(landlordId).filter(UserBankAccount::isMatched)
                    .orElseThrow(() -> new IllegalStateException("Tài khoản ngân hàng nhận tiền của chủ trọ phải ở trạng thái MATCHED trước khi rút tiền!"));
            bankCode = verified.getBankCode();
            accountNumber = verified.getAccountNumber();
            accountHolder = verified.getAccountHolder();
        }
        RiskProfile riskProfile = riskProfileRepository.findByUserId(landlordId).orElseGet(() -> riskProfileRepository.save(RiskProfile.createDefault(landlordId)));
        riskProfile.validateWithdrawal(command.amount(), LocalDate.now());
        long availableBalance = ledgerService.getAccountBalance(ledgerService.getLandlordAvailableAccountNumber(landlordId));
        if (availableBalance < command.amount()) throw new IllegalStateException("Số dư ví khả dụng không đủ (Hiện có: " + availableBalance + " VND)");

        String payoutCode = "PO-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String maskedAcc = PayoutLedgerHelper.maskAccount(accountNumber);
        PayoutLedgerHelper.postHoldOnRequest(ledgerService, landlordId, payoutCode, bankCode, maskedAcc, command.amount());
        boolean needsApproval = command.amount() >= makerCheckerThresholdVnd && makerCheckerRepository != null;
        String mcId = needsApproval ? makerCheckerRepository.save(MakerCheckerRequest.create(landlordId, "PAYOUT_APPROVAL", command.amount(),
                "Rut tien gia tri cao >= " + makerCheckerThresholdVnd + " VND", String.format("{\"payoutCode\":\"%s\",\"bankCode\":\"%s\"}", payoutCode, bankCode))).getId() : null;
        PayoutRequest payout = PayoutRequest.create(payoutCode, landlordId, command.amount(), bankCode, accountNumber, accountHolder, true, needsApproval, mcId);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) payout.setIdempotencyKey(idempotencyKey.trim());
        PayoutRequest saved = payoutRepository.save(payout);
        emitOutbox(needsApproval ? "PAYOUT_PENDING_APPROVAL" : "PAYOUT_REQUESTED", saved);
        if (notificationPort != null) notificationPort.notifyAdminPayoutRequested(landlordId, saved.getPayoutCode(), saved.getAmount(), bankCode, maskedAcc);
        return PayoutLedgerHelper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public PayoutDto.PayoutResponse approvePayout(String payoutId, String checkerAdminId) {
        assertDisbursementAllowed();
        PayoutRequest payout = requirePayout(payoutId);
        if ("PAID".equals(payout.getStatus()) || "COMPLETED".equals(payout.getStatus()) || "APPROVED".equals(payout.getStatus())) return PayoutLedgerHelper.toResponse(payout);
        if (payout.getMakerCheckerRequestId() != null && makerCheckerRepository != null) {
            MakerCheckerRequest mc = makerCheckerRepository.findById(payout.getMakerCheckerRequestId()).orElseThrow(() -> new IllegalStateException("Thieu ban ghi Maker-Checker"));
            mc.approve(checkerAdminId);
            makerCheckerRepository.save(mc);
        } else if (payout.getLandlordId().equals(checkerAdminId)) {
            throw new AccessDeniedException("Khong the tu duyet yeu cau rut tien cua chinh minh");
        }
        payout.approveBy(checkerAdminId);
        PayoutRequest saved = payoutRepository.save(payout);
        emitOutbox("PAYOUT_APPROVED", saved);
        return PayoutLedgerHelper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public PayoutDto.PayoutResponse confirmPayoutPaid(String payoutId, String adminId, PayoutDto.ConfirmPayoutPaidCommand cmd) {
        assertDisbursementAllowed();
        PayoutRequest payout = requirePayout(payoutId);
        if ("PAID".equals(payout.getStatus()) || "COMPLETED".equals(payout.getStatus())) return PayoutLedgerHelper.toResponse(payout);
        if ("REJECTED".equals(payout.getStatus()) || "CANCELLED".equals(payout.getStatus())) throw new IllegalStateException("Không thể xác nhận đã chuyển cho yêu cầu đã hủy hoặc từ chối");
        if (payout.isRequiresMakerChecker() && "PENDING_APPROVAL".equals(payout.getStatus())) throw new IllegalStateException("Yêu cầu rút tiền vượt ngưỡng phải được duyệt Maker-Checker (APPROVED) trước khi xác nhận đã chuyển!");
        String normTx = PaymentReferenceCodeCodec.normalizeAlphanumericUpper(cmd != null ? cmd.bankTxCode() : null);
        if (normTx.length() < 6 || normTx.length() > 36) throw new IllegalArgumentException("Mã giao dịch ngân hàng (FT...) không hợp lệ (cần 6-36 ký tự chữ/số)");
        if (bankTxRegistry != null && bankTxRegistry.existsByNormalizedTxCode(normTx)) throw new IllegalStateException("Phát hiện trùng lặp: Mã giao dịch ngân hàng [" + normTx + "] đã được sử dụng trước đó!");
        if (bankTxRegistry != null) {
            bankTxRegistry.save(BankTransactionRecord.createOutflow(normTx, "ADMIN_PAYOUT", payout.getAmount(), payout.getPayoutCode(),
                    "PAYOUT", payout.getId(), PayoutLedgerHelper.maskAccount(payout.getAccountNumber()),
                    cmd != null && cmd.note() != null ? cmd.note() : "Admin giải ngân rút tiền", adminId, Instant.now()));
        }
        PayoutLedgerHelper.postPaidOutflow(ledgerService, payout, normTx, adminId, PayoutLedgerHelper.maskAccount(payout.getAccountNumber()));
        RiskProfile riskProfile = riskProfileRepository.findByUserId(payout.getLandlordId()).orElseGet(() -> riskProfileRepository.save(RiskProfile.createDefault(payout.getLandlordId())));
        riskProfile.recordWithdrawal(payout.getAmount(), LocalDate.now());
        riskProfileRepository.save(riskProfile);

        String storedProofUrl = cmd != null ? cmd.proofUrl() : null;
        if (storedProofUrl != null && storedProofUrl.startsWith("data:image/") && proofStorageService != null) {
            storedProofUrl = proofStorageService.storePayoutReceiptImage(payout.getId(), storedProofUrl);
        }
        payout.markPaid(normTx, storedProofUrl, adminId);
        PayoutRequest saved = payoutRepository.save(payout);
        emitOutbox("PAYOUT_PAID", saved);
        if (notificationPort != null) notificationPort.notifyLandlordPayoutPaid(saved.getLandlordId(), saved.getPayoutCode(), saved.getAmount(), normTx);
        return PayoutLedgerHelper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public byte[] loadAuthorizedPayoutProofImage(String payoutId, String actorId, boolean isAdmin) {
        PayoutRequest payout = requirePayout(payoutId);
        if (!isAdmin && !payout.getLandlordId().equals(actorId)) throw new WebhookSecurityException(404, "Không tìm thấy biên lai giải ngân");
        if (proofStorageService == null) throw new WebhookSecurityException(404, "Không tìm thấy biên lai giải ngân");
        return proofStorageService.loadPayoutReceiptImage(payout.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public PayoutDto.PayoutResponse cancelPayout(String payoutId, String landlordId) {
        PayoutRequest payout = requirePayout(payoutId);
        if (!payout.getLandlordId().equals(landlordId)) throw new WebhookSecurityException(404, "Không tìm thấy yêu cầu rút tiền: " + payoutId);
        payout.cancelByLandlord();
        PayoutLedgerHelper.postReversalToAvailable(ledgerService, payout, "PAYOUT-CANCEL-" + payout.getPayoutCode(), "Chủ trọ hủy yêu cầu rút tiền", landlordId);
        PayoutRequest saved = payoutRepository.save(payout);
        emitOutbox("PAYOUT_CANCELLED", saved);
        return PayoutLedgerHelper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public PayoutDto.PayoutResponse rejectPayout(String payoutId, String checkerAdminId, String reason) {
        PayoutRequest payout = requirePayout(payoutId);
        if ("PAID".equals(payout.getStatus()) || "COMPLETED".equals(payout.getStatus())) throw new IllegalStateException("Không thể từ chối yêu cầu rút tiền đã hoàn tất");
        if ("REJECTED".equals(payout.getStatus()) || "CANCELLED".equals(payout.getStatus())) return PayoutLedgerHelper.toResponse(payout);
        if (payout.getMakerCheckerRequestId() != null && makerCheckerRepository != null) {
            MakerCheckerRequest mc = makerCheckerRepository.findById(payout.getMakerCheckerRequestId()).orElseThrow(() -> new IllegalStateException("Thieu ban ghi Maker-Checker"));
            mc.reject(checkerAdminId, reason != null ? reason : "Tu choi boi quan tri vien");
            makerCheckerRepository.save(mc);
        }
        String finalReason = reason != null && !reason.isBlank() ? reason : "Tu choi boi quan tri vien";
        PayoutLedgerHelper.postReversalToAvailable(ledgerService, payout, "PAYOUT-REJECT-" + payout.getPayoutCode(), "Hoàn trả số dư do từ chối rút tiền: " + finalReason, checkerAdminId);
        payout.reject(finalReason);
        PayoutRequest saved = payoutRepository.save(payout);
        emitOutbox("PAYOUT_REJECTED", saved);
        return PayoutLedgerHelper.toResponse(saved);
    }

    @Transactional
    public RiskProfile markBankChangedByOwner(String landlordId, String otpCode) {
        verifyOtpCode(landlordId, otpCode);
        RiskProfile profile = riskProfileRepository.findByUserId(landlordId).orElseGet(() -> riskProfileRepository.save(RiskProfile.createDefault(landlordId)));
        profile.markBankChanged(Instant.now());
        RiskProfile saved = riskProfileRepository.save(profile);
        if (outboxRepository != null) outboxRepository.save(BillingEventOutbox.create("BANK_ACCOUNT_CHANGED", "RISK_PROFILE", landlordId, "{\"landlordId\":\"" + landlordId + "\"}"));
        return saved;
    }

    @Transactional(readOnly = true) public List<PayoutDto.PayoutResponse> getAllPayouts() { return payoutRepository.findAll().stream().map(PayoutLedgerHelper::toResponse).toList(); }
    @Transactional(readOnly = true) public List<PayoutDto.PayoutResponse> getLandlordPayouts(String landlordId) { return payoutRepository.findByLandlordId(landlordId).stream().map(PayoutLedgerHelper::toResponse).toList(); }

    private PayoutRequest requirePayout(String payoutId) { return payoutRepository.findById(payoutId).orElseThrow(() -> new WebhookSecurityException(404, "Không tìm thấy yêu cầu rút tiền: " + payoutId)); }
    private void assertDisbursementAllowed() { if ("LIVE".equalsIgnoreCase(fintechMode) && !liveDisbursementEnabled) throw new IllegalStateException("BLOCKED_HUMAN_REVIEW [LEGAL-02]: Live disbursement disabled without license/partner sign-off."); }
    private void verifyOtpCode(String userId, String otp) {
        if (otp == null || otp.trim().isEmpty()) throw new AccessDeniedException("Yêu cầu mã TOTP để thực hiện giao dịch này");
        if (!otp.matches("^[0-9]{6,8}$")) throw new AccessDeniedException("Mã OTP xác thực phải gồm 6 chữ số hợp lệ.");
        if (identityVerificationPort != null && !identityVerificationPort.verifyOtp(userId, otp)) throw new AccessDeniedException("Mã OTP xác thực không chính xác hoặc đã hết hạn");
    }
    private void emitOutbox(String eventType, PayoutRequest p) {
        if (outboxRepository == null) return;
        outboxRepository.save(BillingEventOutbox.create(eventType, "PAYOUT", p.getId(),
                String.format("{\"payoutId\":\"%s\",\"payoutCode\":\"%s\",\"landlordId\":\"%s\",\"amount\":%d,\"status\":\"%s\"}",
                        p.getId(), p.getPayoutCode(), p.getLandlordId(), p.getAmount(), p.getStatus())));
    }
}
