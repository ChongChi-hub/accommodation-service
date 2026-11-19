package vn.cozynest.billing.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.exception.WebhookSecurityException;
import vn.cozynest.billing.application.port.NotificationProvider;
import vn.cozynest.billing.application.port.PaymentGatewayPort;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BankInflowWebhookService {
    private final NotificationProvider notificationProvider;
    private final PaymentIntentRepository paymentIntentRepository;
    private final BankTransactionRegistryRepository bankTxRegistry;
    private final UnmatchedBankInflowRepository unmatchedRepo;
    private final PaymentWebhookLogRepository webhookLogRepo;
    private final BillingEventOutboxRepository outboxRepo;
    private final MakerCheckerRepository makerCheckerRepo;
    private final PaymentLedgerPoster ledgerPoster;
    private final PaymentGatewayPort gatewayPort;
    private final String platformAccountNumber;
    private final long manualMakerCheckerThresholdVnd;

    @Autowired
    public BankInflowWebhookService(
            NotificationProvider notificationProvider,
            PaymentIntentRepository paymentIntentRepository,
            BankTransactionRegistryRepository bankTxRegistry,
            UnmatchedBankInflowRepository unmatchedRepo,
            @Autowired(required = false) PaymentWebhookLogRepository webhookLogRepo,
            @Autowired(required = false) BillingEventOutboxRepository outboxRepo,
            @Autowired(required = false) MakerCheckerRepository makerCheckerRepo,
            PaymentLedgerPoster ledgerPoster,
            @Autowired(required = false) PaymentGatewayPort gatewayPort,
            @Value("${fintech.platform.account-number:1042701900}") String platformAccountNumber,
            @Value("${fintech.payment.manual-maker-checker-threshold-vnd:10000000}") long manualMakerCheckerThresholdVnd) {
        this.notificationProvider = notificationProvider;
        this.paymentIntentRepository = paymentIntentRepository;
        this.bankTxRegistry = bankTxRegistry;
        this.unmatchedRepo = unmatchedRepo;
        this.webhookLogRepo = webhookLogRepo;
        this.outboxRepo = outboxRepo;
        this.makerCheckerRepo = makerCheckerRepo;
        this.ledgerPoster = ledgerPoster;
        this.gatewayPort = gatewayPort;
        this.platformAccountNumber = platformAccountNumber != null ? platformAccountNumber.trim() : "1042701900";
        this.manualMakerCheckerThresholdVnd = manualMakerCheckerThresholdVnd > 0 ? manualMakerCheckerThresholdVnd : 10_000_000L;
    }

    @Transactional(rollbackFor = Exception.class)
    public WebhookOutcome processProviderWebhook(String rawBody, String signature, long timestamp,
                                                 String sourceIp, Map<String, String> headers) {
        if (gatewayPort != null && gatewayPort.isKillSwitchActive()) {
            throw new IllegalStateException("Cổng thanh toán đang tạm khóa");
        }
        String sanitized = notificationProvider.sanitizePayload(rawBody);
        if (!notificationProvider.isTimestampWithinWindow(timestamp)) {
            logWebhook(null, sourceIp, false, "TIMESTAMP_EXPIRED", "Timestamp out of window", sanitized);
            throw new IllegalArgumentException("Webhook timestamp nằm ngoài cửa sổ cho phép");
        }
        if (!notificationProvider.verifySignature(rawBody, signature, timestamp)) {
            logWebhook(null, sourceIp, false, "INVALID_SIGNATURE", "HMAC signature mismatch", sanitized);
            throw new WebhookSecurityException(401, "Chữ ký Webhook biến động số dư không hợp lệ");
        }

        NotificationProvider.ParsedBankNotification parsed = notificationProvider.parseNotification(rawBody, headers);
        if (!"IN".equalsIgnoreCase(parsed.direction())) {
            logWebhook(null, sourceIp, true, "DIRECTION_REJECTED", "Not an IN transaction", sanitized);
            throw new IllegalArgumentException("Webhook bị từ chối: Chỉ chấp nhận giao dịch chiều TIỀN VÀO (IN)");
        }
        String cleanReceiver = parsed.receiverAccountNumber() != null
                ? parsed.receiverAccountNumber().replaceAll("[^0-9A-Za-z]", "") : "";
        if (!platformAccountNumber.equalsIgnoreCase(cleanReceiver)) {
            logWebhook(null, sourceIp, true, "WRONG_RECEIVER_ACCOUNT", "Receiver account mismatch", sanitized);
            throw new IllegalArgumentException("Webhook bị từ chối: Số tài khoản nhận không khớp tài khoản sàn Tổ Ấm");
        }

        String normTxId = PaymentReferenceCodeCodec.normalizeAlphanumericUpper(parsed.providerTxId());
        if (normTxId.isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã giao dịch ngân hàng (providerTxId)");
        }
        if (bankTxRegistry.existsByNormalizedTxCode(normTxId) || unmatchedRepo.existsByProviderTxId(normTxId)) {
            logWebhook(null, sourceIp, true, "IDEMPOTENT_REPLAY_IGNORED", "Already processed txId=" + normTxId, sanitized);
            return new WebhookOutcome("IDEMPOTENT_REPLAY_IGNORED", false, null, normTxId, "Giao dịch đã được xử lý trước đó");
        }

        String maskedReceiver = PaymentReferenceCodeCodec.maskAccountNumber(cleanReceiver);
        Optional<String> extractedRef = PaymentReferenceCodeCodec.extractReferenceFromBankContent(parsed.transferContent());
        Optional<PaymentIntent> matchedIntent = findMatchingIntent(parsed, extractedRef.orElse(null));

        if (matchedIntent.isEmpty()) {
            UnmatchedBankInflow queued = unmatchedRepo.save(UnmatchedBankInflow.create(
                    parsed.provider(), normTxId, parsed.amount(), parsed.transferContent(),
                    extractedRef.orElse(null), "MISSING_OR_UNKNOWN_REF", null, maskedReceiver, sanitized));
            logWebhook(null, sourceIp, true, "QUEUED_UNMATCHED_REF", queued.getMismatchReason(), sanitized);
            return new WebhookOutcome("QUEUED_UNMATCHED", false, null, normTxId, "Không tìm thấy mã tham chiếu hợp lệ, đã đưa vào hàng đợi Tiền về chưa khớp");
        }

        PaymentIntent intent = matchedIntent.get();
        Instant now = Instant.now();
        String mismatchReason = evaluateMismatchReason(intent, parsed.amount(), now);
        if (mismatchReason != null) {
            unmatchedRepo.save(UnmatchedBankInflow.create(
                    parsed.provider(), normTxId, parsed.amount(), parsed.transferContent(),
                    extractedRef.orElse(intent.getPaymentCode()), mismatchReason,
                    intent.getPaymentCode(), maskedReceiver, sanitized));
            logWebhook(intent.getPaymentCode(), sourceIp, true, "QUEUED_" + mismatchReason, mismatchReason, sanitized);
            return new WebhookOutcome("QUEUED_UNMATCHED", false, intent.getPaymentCode(), normTxId,
                    "Giao dịch không khớp điều kiện tự động (" + mismatchReason + "), đã chuyển vào hàng đợi Admin");
        }

        bankTxRegistry.save(BankTransactionRecord.createInflow(
                normTxId, parsed.provider(), parsed.amount(), intent.getPaymentCode(),
                intent.getOrderType(), intent.getOrderId(), maskedReceiver,
                parsed.transferContent(), sanitized, "WEBHOOK_" + parsed.provider(), parsed.occurredAt()));

        intent.markPaid(parsed.amount());
        PaymentIntent savedIntent = paymentIntentRepository.save(intent);
        ledgerPoster.postLedgerForPaymentWithProof(
                savedIntent, parsed.amount(), "BANKTX-" + normTxId, "BANK_TX:" + normTxId, null);
        emitOutbox(savedIntent, "PAYMENT_CONFIRMED");
        logWebhook(savedIntent.getPaymentCode(), sourceIp, true, "CONFIRMED_MATCHED", null, sanitized);
        return new WebhookOutcome("CONFIRMED", true, savedIntent.getPaymentCode(), normTxId, "Đã xác minh tiền về và ghi sổ thành công");
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentIntent confirmManualByAdmin(String paymentCode, String rawBankTxCode, long actualAmount,
                                              String reason, String adminId, String checkerAdminId) {
        String normTx = PaymentReferenceCodeCodec.normalizeAlphanumericUpper(rawBankTxCode);
        if (normTx.length() < 6 || normTx.length() > 36) {
            throw new IllegalArgumentException("Mã giao dịch ngân hàng không hợp lệ (6-36 ký tự chữ/số)");
        }
        if (reason == null || reason.trim().length() < 3) {
            throw new IllegalArgumentException("Bắt buộc nhập lý do xác nhận thủ công");
        }
        if (bankTxRegistry.existsByNormalizedTxCode(normTx)) {
            throw new IllegalStateException("Phát hiện trùng lặp: Mã giao dịch ngân hàng [" + normTx + "] đã tồn tại trong registry!");
        }
        PaymentIntent intent = paymentIntentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new WebhookSecurityException(404, "Không tìm thấy mã thanh toán: " + paymentCode));
        if ("PAID".equals(intent.getStatus()) || "OVERPAID".equals(intent.getStatus())) {
            throw new IllegalStateException("Khoản thanh toán này đã được xác nhận trước đó");
        }
        if (actualAmount != intent.getAmount()) {
            throw new IllegalArgumentException("Số tiền xác nhận (" + actualAmount + ") phải khớp số tiền khoản thanh toán (" + intent.getAmount() + ")");
        }

        if (actualAmount >= manualMakerCheckerThresholdVnd) {
            if (checkerAdminId == null || checkerAdminId.isBlank() || checkerAdminId.equals(adminId)) {
                if (makerCheckerRepo != null) {
                    makerCheckerRepo.save(MakerCheckerRequest.create(
                            adminId, "MANUAL_PAYMENT_CONFIRM", actualAmount, reason,
                            String.format("{\"paymentCode\":\"%s\",\"bankTxCode\":\"%s\"}", paymentCode, normTx)));
                }
                throw new IllegalStateException("MAKER_CHECKER_REQUIRED: Khoản xác nhận thủ công từ "
                        + manualMakerCheckerThresholdVnd + " VND trở lên cần quản trị viên thứ hai phê duyệt!");
            }
        }

        bankTxRegistry.save(BankTransactionRecord.createInflow(
                normTx, "ADMIN_MANUAL", actualAmount, intent.getPaymentCode(),
                intent.getOrderType(), intent.getOrderId(),
                PaymentReferenceCodeCodec.maskAccountNumber(platformAccountNumber),
                reason.trim(), null, adminId, Instant.now()));

        unmatchedRepo.findByProviderTxId(normTx).ifPresent(u -> {
            u.resolve(paymentCode, adminId, reason.trim(), "RESOLVED_CONFIRMED");
            unmatchedRepo.save(u);
        });

        intent.markPaid(actualAmount);
        PaymentIntent saved = paymentIntentRepository.save(intent);
        String secureProofUrl = "/api/v1/billing/payments/proofs/" + saved.getPaymentCode() + "/image";
        ledgerPoster.postLedgerForPaymentWithProof(saved, actualAmount, "BANKTX-" + normTx, "BANK_TX:" + normTx, secureProofUrl);
        emitOutbox(saved, "PAYMENT_CONFIRMED_MANUAL");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<UnmatchedBankInflow> listUnmatchedInflows(String statusFilter) {
        if (statusFilter != null && !statusFilter.isBlank()) {
            return unmatchedRepo.findByStatusOrderByReceivedAtDesc(statusFilter.trim().toUpperCase());
        }
        return unmatchedRepo.findAllOrderByReceivedAtDesc();
    }

    private String evaluateMismatchReason(PaymentIntent intent, long inflowAmount, Instant now) {
        if ("PAID".equals(intent.getStatus()) || "OVERPAID".equals(intent.getStatus())) return "ALREADY_PAID_DUPLICATE";
        if ("CANCELLED".equals(intent.getStatus())) return "INTENT_CANCELLED";
        if ("EXPIRED".equals(intent.getStatus()) || (intent.getExpiresAt() != null && intent.getExpiresAt().isBefore(now))) return "EXPIRED_INTENT";
        if (!"PENDING".equals(intent.getStatus()) && !"SUBMITTED".equals(intent.getStatus())) return "INVALID_INTENT_STATUS";
        if (inflowAmount != intent.getAmount()) return "AMOUNT_MISMATCH";
        return null;
    }

    private Optional<PaymentIntent> findMatchingIntent(NotificationProvider.ParsedBankNotification parsed, String extractedRef) {
        if (parsed.ExplicitPaymentCode() != null && !parsed.ExplicitPaymentCode().isBlank()) {
            Optional<PaymentIntent> byExplicit = paymentIntentRepository.findByPaymentCode(parsed.ExplicitPaymentCode().trim());
            if (byExplicit.isPresent()) return byExplicit;
        }
        if (extractedRef != null && !extractedRef.isBlank()) {
            Optional<PaymentIntent> byRef = paymentIntentRepository.findByPaymentCode(extractedRef);
            if (byRef.isPresent()) return byRef;
        }
        String rawContent = parsed.transferContent();
        if (rawContent == null || rawContent.isBlank()) return Optional.empty();
        return paymentIntentRepository.findAll().stream()
                .filter(i -> PaymentReferenceCodeCodec.matchesPaymentCode(rawContent, i.getPaymentCode()))
                .findFirst();
    }

    private void logWebhook(String paymentCode, String ip, boolean sigValid, String status, String err, String sanitizedRaw) {
        if (webhookLogRepo == null) return;
        webhookLogRepo.save(new PaymentWebhookLog(null, paymentCode, ip, sigValid, status, err,
                sanitizedRaw != null ? sanitizedRaw : "", Instant.now()));
    }

    private void emitOutbox(PaymentIntent intent, String eventType) {
        if (outboxRepo == null) return;
        String payload = String.format(
                "{\"paymentCode\":\"%s\",\"orderType\":\"%s\",\"orderId\":\"%s\",\"tenantId\":\"%s\",\"paidAmount\":%d,\"status\":\"%s\"}",
                intent.getPaymentCode(), intent.getOrderType(), intent.getOrderId(),
                intent.getPayerId(), intent.getPaidAmount(), intent.getStatus());
        outboxRepo.save(BillingEventOutbox.create(eventType, "PAYMENT", intent.getId(), payload));
    }

    public record WebhookOutcome(String status, boolean autoConfirmed, String paymentCode, String providerTxId, String message) {}
}

