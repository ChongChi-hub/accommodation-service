package vn.cozynest.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.PaymentDto;
import vn.cozynest.billing.application.exception.ObligationAlreadyPaidException;
import vn.cozynest.billing.application.exception.WebhookSecurityException;
import vn.cozynest.billing.application.port.BillingContractPort;
import vn.cozynest.billing.application.port.BillingNotificationPort;
import vn.cozynest.billing.application.port.PaymentGatewayPort;
import vn.cozynest.billing.domain.model.PaymentIntent;
import vn.cozynest.billing.domain.model.PaymentReferenceCodeCodec;
import vn.cozynest.billing.domain.repository.*;

import java.time.Instant;
import java.util.*;

@Service
public class PaymentService {
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentGatewayPort gatewayPort;
    private final PaymentWebhookVerifier verifier;
    private final PaymentLedgerPoster ledgerPoster;
    private final PaymentObligationValidator validator;
    private final PaymentWebhookProcessor webhookProcessor;
    private final BankAccountService bankAccountService;
    private final PaymentProofStorageService proofStorageService;
    private final BankInflowWebhookService bankInflowWebhookService;
    private final String fintechMode;

    public PaymentService(PaymentIntentRepository paymentIntentRepository, InvoiceRepository invoiceRepository,
            LedgerService ledgerService, ObjectMapper objectMapper, PaymentGatewayPort gatewayPort,
            PaymentWebhookLogRepository webhookLogRepository, String webhookSecret,
            String previousWebhookSecret, boolean strictIpAllowlist, String allowedIpsRaw) {
        this(paymentIntentRepository, invoiceRepository, ledgerService, objectMapper, gatewayPort,
                webhookLogRepository, Optional.empty(), webhookSecret, previousWebhookSecret, strictIpAllowlist, allowedIpsRaw);
    }
    public PaymentService(PaymentIntentRepository paymentIntentRepository, InvoiceRepository invoiceRepository,
            LedgerService ledgerService, ObjectMapper objectMapper, PaymentGatewayPort gatewayPort,
            PaymentWebhookLogRepository webhookLogRepository, Optional<BillingEventOutboxRepository> outboxRepository,
            String webhookSecret, String previousWebhookSecret, boolean strictIpAllowlist, String allowedIpsRaw) {
        this(paymentIntentRepository, invoiceRepository, ledgerService, objectMapper, gatewayPort,
                webhookLogRepository, outboxRepository, Optional.empty(), Optional.empty(),
                webhookSecret, previousWebhookSecret, strictIpAllowlist, allowedIpsRaw);
    }
    public PaymentService(PaymentIntentRepository paymentIntentRepository, InvoiceRepository invoiceRepository,
            LedgerService ledgerService, ObjectMapper objectMapper, PaymentGatewayPort gatewayPort,
            PaymentWebhookLogRepository webhookLogRepository, Optional<BillingEventOutboxRepository> outboxRepository,
            Optional<EscrowService> escrowService, Optional<BillingContractPort> contractPort,
            String webhookSecret, String previousWebhookSecret, boolean strictIpAllowlist, String allowedIpsRaw) {
        this(paymentIntentRepository, invoiceRepository, ledgerService, objectMapper, gatewayPort,
                webhookLogRepository, outboxRepository, escrowService, contractPort,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                "SANDBOX_ONLY", webhookSecret, previousWebhookSecret, strictIpAllowlist, allowedIpsRaw);
    }

    @Autowired
    public PaymentService(PaymentIntentRepository paymentIntentRepository, InvoiceRepository invoiceRepository,
            LedgerService ledgerService, ObjectMapper objectMapper, PaymentGatewayPort gatewayPort,
            PaymentWebhookLogRepository webhookLogRepository, Optional<BillingEventOutboxRepository> outboxRepository,
            Optional<EscrowService> escrowService, Optional<BillingContractPort> contractPort,
            Optional<BillingNotificationPort> notificationPort, Optional<BankAccountService> bankAccountService,
            Optional<PaymentProofStorageService> proofStorageService, Optional<BankInflowWebhookService> bankInflowWebhookService,
            @Value("${fintech.mode:SANDBOX_ONLY}") String fintechMode,
            @Value("${fintech.gateway.webhook-secret:cozynest-dev-fake-webhook-secret-2026}") String webhookSecret,
            @Value("${fintech.gateway.webhook-secret-previous:}") String previousWebhookSecret,
            @Value("${fintech.gateway.strict-ip-allowlist:false}") boolean strictIpAllowlist,
            @Value("${fintech.gateway.allowed-ips:127.0.0.1,::1}") String allowedIpsRaw) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.gatewayPort = gatewayPort;
        this.bankAccountService = bankAccountService != null ? bankAccountService.orElse(null) : null;
        this.proofStorageService = proofStorageService != null ? proofStorageService.orElse(null) : null;
        this.bankInflowWebhookService = bankInflowWebhookService != null ? bankInflowWebhookService.orElse(null) : null;
        this.fintechMode = fintechMode != null ? fintechMode : "SANDBOX_ONLY";
        this.verifier = new PaymentWebhookVerifier(webhookSecret, previousWebhookSecret, strictIpAllowlist, allowedIpsRaw);
        this.ledgerPoster = new PaymentLedgerPoster(ledgerService, invoiceRepository,
                escrowService != null ? escrowService.orElse(null) : null, contractPort != null ? contractPort.orElse(null) : null,
                notificationPort != null ? notificationPort.orElse(null) : null, this.fintechMode, false);
        this.validator = new PaymentObligationValidator(invoiceRepository, null, contractPort != null ? contractPort.orElse(null) : null);
        this.webhookProcessor = new PaymentWebhookProcessor(paymentIntentRepository, webhookLogRepository, gatewayPort,
                verifier, ledgerPoster, objectMapper, outboxRepository != null ? outboxRepository.orElse(null) : null);
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentDto.IntentResponse createIntent(String payerId, PaymentDto.CreateIntentRequest request) {
        if (gatewayPort != null && gatewayPort.isKillSwitchActive()) throw new IllegalStateException("Cổng thanh toán đang tạm khóa");
        if (bankAccountService != null && "DEPOSIT".equalsIgnoreCase(request.orderType())) bankAccountService.requireValidBankAccount(payerId, "Khách thuê");
        long effectiveAmount = request.amount() != null ? request.amount() : 0L;
        Instant now = Instant.now();
        if ("DEPOSIT".equalsIgnoreCase(request.orderType())) effectiveAmount = validator.validateAndResolveDepositAmount(request.orderId(), payerId, request.amount());
        else if ("INVOICE".equalsIgnoreCase(request.orderType())) validator.validateInvoiceObligation(request.orderId());
        for (PaymentIntent existing : paymentIntentRepository.findByOrderIdAndOrderType(request.orderId(), request.orderType())) {
            if ("PAID".equals(existing.getStatus()) || "OVERPAID".equals(existing.getStatus())) throw new ObligationAlreadyPaidException("Khoản tiền này đã được thanh toán thành công");
        }
        for (PaymentIntent existing : paymentIntentRepository.findByOrderIdAndOrderTypeAndStatus(request.orderId(), request.orderType(), "PENDING")) {
            if (existing.markExpiredIfPast(now)) paymentIntentRepository.save(existing);
            else throw new IllegalStateException("Hợp đồng này đang có giao dịch chờ thanh toán (" + existing.getPaymentCode() + "). Vui lòng hoàn tất hoặc hủy giao dịch cũ.");
        }
        if (effectiveAmount < 1000L) throw new IllegalArgumentException("Số tiền thanh toán tối thiểu là 1,000 VND");
        String paymentCode = generateUniqueReferenceCode();
        String qrUrl = gatewayPort.buildPaymentInstructionUrl(paymentCode, effectiveAmount, request.orderType());
        return toResponse(paymentIntentRepository.save(PaymentIntent.create(paymentCode, payerId, request.orderType(), request.orderId(), effectiveAmount, qrUrl, 15)));
    }
    private String generateUniqueReferenceCode() {
        for (int i = 0; i < 5; i++) { String c = PaymentReferenceCodeCodec.generateReferenceCode(); if (paymentIntentRepository.findByPaymentCode(c).isEmpty()) return c; }
        return PaymentReferenceCodeCodec.generateReferenceCode();
    }
    @Transactional(rollbackFor = Exception.class)
    public PaymentDto.IntentResponse submitIntent(String paymentCode, String actorId, PaymentDto.SubmitIntentRequest req) {
        PaymentIntent intent = paymentIntentRepository.findByPaymentCode(paymentCode).orElseThrow(() -> new WebhookSecurityException(404, "Không tìm thấy mã thanh toán: " + paymentCode));
        if (!actorId.equals(intent.getPayerId())) throw new WebhookSecurityException(403, "Không có quyền gửi thông báo chuyển khoản cho mã này");
        if ("PAID".equals(intent.getStatus()) || "OVERPAID".equals(intent.getStatus()) || "SUBMITTED".equals(intent.getStatus())) return toResponse(intent);
        if ("CANCELLED".equals(intent.getStatus()) || "EXPIRED".equals(intent.getStatus()) || (intent.getExpiresAt() != null && intent.getExpiresAt().isBefore(Instant.now()))) {
            throw new IllegalStateException("Giao dịch đã bị hủy hoặc hết hạn, không thể báo đã chuyển tiền");
        }
        if (gatewayPort != null && gatewayPort.isKillSwitchActive()) throw new IllegalStateException("Cổng thanh toán Sàn đang tạm khóa");
        if (req != null && req.note() != null && req.note().contains("UNVERIFIED_TRANSFER")) throw new IllegalStateException("Chưa ghi nhận được biến động tiền cọc về tài khoản Sàn: Bắt buộc phải cung cấp Mã giao dịch ngân hàng và Ảnh biên lai chuyển khoản hợp lệ!");
        if (proofStorageService != null && req != null && req.receiptUrl() != null && req.receiptUrl().startsWith("data:image/")) {
            proofStorageService.storeSupportingProof(intent.getPaymentCode(), intent.getOrderType(), intent.getOrderId(), actorId, extractBankTxFromNote(req.note()), extractHashFromNote(req.note()), req.receiptUrl());
        }
        intent.markSubmitted();
        PaymentIntent saved = paymentIntentRepository.save(intent);
        webhookProcessor.publishPaymentEvent(saved, "PAYMENT_SUBMITTED");
        return toResponse(saved);
    }
    @Transactional(rollbackFor = Exception.class)
    public PaymentDto.IntentResponse confirmPaymentByLandlordOrAdmin(String paymentCode, PaymentDto.ConfirmPaymentRequest req, String actorId, boolean isAdmin) {
        if (!isAdmin) throw new WebhookSecurityException(403, "Chỉ Quản trị viên (ADMIN) mới có quyền xác nhận thủ công tiền về tài khoản Sàn");
        if (bankInflowWebhookService != null) {
            return toResponse(bankInflowWebhookService.confirmManualByAdmin(paymentCode, req.bankTxCode(), req.actualAmount(),
                    req.note() != null && !req.note().isBlank() ? req.note() : "Admin xác nhận thủ công sao kê ngân hàng", actorId, null));
        }
        PaymentIntent intent = paymentIntentRepository.findByPaymentCode(paymentCode).orElseThrow(() -> new WebhookSecurityException(404, "Không tìm thấy mã thanh toán: " + paymentCode));
        if ("PAID".equals(intent.getStatus()) || "OVERPAID".equals(intent.getStatus())) return toResponse(intent);
        if ("CANCELLED".equals(intent.getStatus()) || "EXPIRED".equals(intent.getStatus())) throw new IllegalStateException("Giao dịch đã bị hủy hoặc hết hạn, không thể xác nhận");
        String normTx = PaymentReferenceCodeCodec.normalizeAlphanumericUpper(req.bankTxCode());
        if (normTx.length() < 6) throw new IllegalArgumentException("Mã giao dịch ngân hàng không hợp lệ");
        if (req.actualAmount() < intent.getAmount()) { intent.recordGatewayPayment(req.actualAmount(), Instant.now()); webhookProcessor.publishPaymentEvent(intent, "PAYMENT_UNDERPAID"); return toResponse(paymentIntentRepository.save(intent)); }
        intent.markPaid(req.actualAmount());
        PaymentIntent saved = paymentIntentRepository.save(intent);
        ledgerPoster.postLedgerForPayment(saved, req.actualAmount(), "BANKTX-" + normTx);
        webhookProcessor.publishPaymentEvent(saved, "PAYMENT_CONFIRMED");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentDto.IntentResponse> getActivePendingIntentForOrder(String orderType, String orderId, String actorId, boolean isAdmin) {
        Instant now = Instant.now();
        return paymentIntentRepository.findByOrderIdAndOrderType(orderId, orderType).stream()
                .filter(i -> "PENDING".equals(i.getStatus()) || "SUBMITTED".equals(i.getStatus()))
                .filter(i -> i.getExpiresAt() == null || i.getExpiresAt().isAfter(now))
                .filter(i -> isAdmin || actorId.equals(i.getPayerId()))
                .findFirst().map(this::toResponse);
    }

    @Transactional
    public PaymentDto.IntentResponse cancelIntent(String paymentCode, String actorId, boolean isAdmin) {
        PaymentIntent intent = paymentIntentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã thanh toán: " + paymentCode));
        if (!isAdmin && !intent.getPayerId().equals(actorId)) throw new SecurityException("Không có quyền hủy giao dịch");
        intent.cancel();
        return toResponse(paymentIntentRepository.save(intent));
    }

    @Transactional(readOnly = true)
    public PaymentDto.IntentResponse getIntent(String paymentCode, String actorId, boolean isAdmin) {
        PaymentIntent intent = paymentIntentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new WebhookSecurityException(404, "Khong tim thay ma thanh toan: " + paymentCode));
        if (!isAdmin && !intent.getPayerId().equals(actorId)) throw new WebhookSecurityException(404, "Khong tim thay ma: " + paymentCode);
        return toResponse(intent);
    }

    @Transactional(readOnly = true)
    public List<PaymentDto.IntentResponse> getManualInterventionQueue() {
        return paymentIntentRepository.findAll().stream()
                .filter(i -> "SUBMITTED".equals(i.getStatus()) || "OVERPAID".equals(i.getStatus()) || "MISMATCH".equals(i.getStatus())
                        || ("EXPIRED".equals(i.getStatus()) && i.getPaidAmount() > 0) || "PARTIAL_PAID".equals(i.getStatus()))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentDto.AdminIntentResponse> getAllAdminIntents(int limit) {
        int cap = Math.max(1, Math.min(limit, 500));
        return paymentIntentRepository.findAll().stream()
                .sorted(Comparator.comparing(PaymentIntent::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(cap)
                .map(i -> new PaymentDto.AdminIntentResponse(i.getId(), i.getPaymentCode(), i.getPayerId(),
                        i.getOrderType(), i.getOrderId(), i.getAmount(), i.getPaidAmount(), i.getStatus(), i.getExpiresAt(), i.getCreatedAt()))
                .toList();
    }

    @Transactional
    public PaymentDto.IntentResponse simulateSandboxWebhook(String paymentCode, long amount, String actorId, boolean isAdmin) {
        if (!"SANDBOX_ONLY".equalsIgnoreCase(fintechMode)) throw new WebhookSecurityException(404, "Sandbox webhook bị vô hiệu hóa ở chế độ " + fintechMode);
        if (!isAdmin) throw new WebhookSecurityException(403, "Chức năng giả lập chỉ dành cho Quản trị viên hệ thống");
        PaymentIntent intent = paymentIntentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay ma thanh toan: " + paymentCode));
        long payAmount = amount > 0 ? amount : intent.getAmount();
        long ts = Instant.now().getEpochSecond();
        String rawBody = String.format("{\"paymentCode\":\"%s\",\"amount\":%d,\"status\":\"SUCCESS\",\"timestamp\":%d}", paymentCode, payAmount, ts);
        processWebhook(rawBody, verifier.computeHmacSha256(rawBody + "." + ts, verifier.getWebhookSecret()), ts, "127.0.0.1");
        return paymentIntentRepository.findByPaymentCode(paymentCode).map(this::toResponse).orElseThrow();
    }

    @Scheduled(fixedDelayString = "${fintech.gateway.expire-poll-ms:60000}")
    @Transactional
    public int expireOverdueIntents() {
        Instant now = Instant.now();
        int expiredCount = 0;
        for (PaymentIntent intent : paymentIntentRepository.findPendingExpiredBefore(now)) {
            if (intent.markExpiredIfPast(now)) { paymentIntentRepository.save(intent); expiredCount++; }
        }
        return expiredCount;
    }

    @Transactional(readOnly = true)
    public List<PaymentDto.IntentResponse> getLandlordSubmittedIntents(String landlordId) {
        return paymentIntentRepository.findByStatus("SUBMITTED").stream()
                .filter(i -> validator.isContractOwnedByLandlord(i.getOrderType(), i.getOrderId(), landlordId))
                .map(this::toResponse).toList();
    }

    @Transactional
    public boolean processWebhook(String rawBody, String signature, long timestamp) { return processWebhook(rawBody, signature, timestamp, "127.0.0.1"); }

    @Transactional
    public boolean processWebhook(String rawBody, String signature, long timestamp, String sourceIp) {
        return webhookProcessor.processWebhook(rawBody, signature, timestamp, sourceIp);
    }

    private String extractBankTxFromNote(String note) {
        if (note == null) return "";
        for (String p : note.split("\\|")) if (p.startsWith("BANK_TX:")) return p.substring("BANK_TX:".length()).trim();
        return note.trim();
    }
    private String extractHashFromNote(String note) {
        if (note == null) return null;
        for (String p : note.split("\\|")) if (p.startsWith("HASH:")) return p.substring("HASH:".length()).trim();
        return null;
    }
    private PaymentDto.IntentResponse toResponse(PaymentIntent i) {
        return new PaymentDto.IntentResponse(i.getId(), i.getPaymentCode(), i.getOrderType(),
                i.getOrderId(), i.getAmount(), i.getPaidAmount(), i.getQrUrl(), i.getStatus(), i.getExpiresAt());
    }
}
