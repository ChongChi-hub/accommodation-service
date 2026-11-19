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
import vn.cozynest.billing.application.port.PaymentGatewayPort;
import vn.cozynest.billing.domain.model.PaymentIntent;
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

    public PaymentService(
            PaymentIntentRepository paymentIntentRepository, InvoiceRepository invoiceRepository,
            LedgerService ledgerService, ObjectMapper objectMapper, PaymentGatewayPort gatewayPort,
            PaymentWebhookLogRepository webhookLogRepository, String webhookSecret,
            String previousWebhookSecret, boolean strictIpAllowlist, String allowedIpsRaw) {
        this(paymentIntentRepository, invoiceRepository, ledgerService, objectMapper, gatewayPort,
                webhookLogRepository, Optional.empty(), webhookSecret, previousWebhookSecret, strictIpAllowlist, allowedIpsRaw);
    }

    public PaymentService(
            PaymentIntentRepository paymentIntentRepository, InvoiceRepository invoiceRepository,
            LedgerService ledgerService, ObjectMapper objectMapper, PaymentGatewayPort gatewayPort,
            PaymentWebhookLogRepository webhookLogRepository, Optional<BillingEventOutboxRepository> outboxRepository,
            String webhookSecret, String previousWebhookSecret, boolean strictIpAllowlist, String allowedIpsRaw) {
        this(paymentIntentRepository, invoiceRepository, ledgerService, objectMapper, gatewayPort,
                webhookLogRepository, outboxRepository, Optional.empty(), Optional.empty(),
                webhookSecret, previousWebhookSecret, strictIpAllowlist, allowedIpsRaw);
    }

    @Autowired
    public PaymentService(
            PaymentIntentRepository paymentIntentRepository, InvoiceRepository invoiceRepository,
            LedgerService ledgerService, ObjectMapper objectMapper, PaymentGatewayPort gatewayPort,
            PaymentWebhookLogRepository webhookLogRepository, Optional<BillingEventOutboxRepository> outboxRepository,
            Optional<EscrowService> escrowService, Optional<BillingContractPort> contractPort,
            @Value("${fintech.gateway.webhook-secret:}") String webhookSecret,
            @Value("${fintech.gateway.webhook-secret-previous:}") String previousWebhookSecret,
            @Value("${fintech.gateway.strict-ip-allowlist:false}") boolean strictIpAllowlist,
            @Value("${fintech.gateway.allowed-ips:127.0.0.1,::1}") String allowedIpsRaw) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.gatewayPort = gatewayPort;
        this.verifier = new PaymentWebhookVerifier(webhookSecret, previousWebhookSecret, strictIpAllowlist, allowedIpsRaw);
        this.ledgerPoster = new PaymentLedgerPoster(ledgerService, invoiceRepository,
                escrowService != null ? escrowService.orElse(null) : null,
                contractPort != null ? contractPort.orElse(null) : null,
                "SANDBOX_ONLY", false);
        this.validator = new PaymentObligationValidator(invoiceRepository, null,
                contractPort != null ? contractPort.orElse(null) : null);
        this.webhookProcessor = new PaymentWebhookProcessor(paymentIntentRepository, webhookLogRepository, gatewayPort,
                verifier, ledgerPoster, objectMapper, outboxRepository != null ? outboxRepository.orElse(null) : null);
    }

    @Transactional
    public PaymentDto.IntentResponse createIntent(String payerId, PaymentDto.CreateIntentRequest request) {
        if (gatewayPort != null && gatewayPort.isKillSwitchActive()) {
            throw new IllegalStateException("Cổng thanh toán đang tạm khóa");
        }
        long effectiveAmount = request.amount() != null ? request.amount() : 0L;
        Instant now = Instant.now();
        if ("DEPOSIT".equalsIgnoreCase(request.orderType())) {
            effectiveAmount = validator.validateAndResolveDepositAmount(request.orderId(), payerId, request.amount());
        } else if ("INVOICE".equalsIgnoreCase(request.orderType())) {
            validator.validateInvoiceObligation(request.orderId());
        }

        for (PaymentIntent existing : paymentIntentRepository.findByOrderIdAndOrderType(request.orderId(), request.orderType())) {
            if ("PAID".equals(existing.getStatus()) || "OVERPAID".equals(existing.getStatus())) {
                throw new ObligationAlreadyPaidException("Khoản tiền này đã được thanh toán thành công");
            }
        }
        for (PaymentIntent existing : paymentIntentRepository.findByOrderIdAndOrderTypeAndStatus(request.orderId(), request.orderType(), "PENDING")) {
            if (existing.markExpiredIfPast(now)) {
                paymentIntentRepository.save(existing);
            } else {
                throw new IllegalStateException("Hợp đồng này đang có giao dịch chờ thanh toán (" + existing.getPaymentCode() + "). Vui lòng hoàn tất hoặc hủy giao dịch cũ.");
            }
        }

        if (effectiveAmount < 1000L) {
            throw new IllegalArgumentException("Số tiền thanh toán tối thiểu là 1,000 VND");
        }

        String paymentCode = "PAY-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String qrUrl = gatewayPort.buildPaymentInstructionUrl(paymentCode, effectiveAmount, request.orderType());
        PaymentIntent saved = paymentIntentRepository.save(PaymentIntent.create(
                paymentCode, payerId, request.orderType(), request.orderId(), effectiveAmount, qrUrl, 15));
        return toResponse(saved);
    }

    @Transactional
    public PaymentDto.IntentResponse submitIntent(String paymentCode, String actorId, PaymentDto.SubmitIntentRequest req) {
        PaymentIntent intent = paymentIntentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new WebhookSecurityException(404, "Không tìm thấy mã thanh toán: " + paymentCode));
        if (!actorId.equals(intent.getPayerId())) {
            throw new WebhookSecurityException(403, "Không có quyền gửi thông báo chuyển khoản cho mã này");
        }
        intent.markSubmitted();
        PaymentIntent saved = paymentIntentRepository.save(intent);
        webhookProcessor.publishPaymentEvent(saved, "PAYMENT_SUBMITTED");
        return toResponse(saved);
    }

    @Transactional
    public PaymentDto.IntentResponse confirmPaymentByLandlordOrAdmin(
            String paymentCode, PaymentDto.ConfirmPaymentRequest req, String actorId, boolean isAdmin) {
        PaymentIntent intent = paymentIntentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new WebhookSecurityException(404, "Không tìm thấy mã thanh toán: " + paymentCode));
        if (!isAdmin) {
            validator.validateLandlordPermission(intent.getOrderType(), intent.getOrderId(), actorId);
        }
        if ("PAID".equals(intent.getStatus()) || "OVERPAID".equals(intent.getStatus())) {
            return toResponse(intent);
        }
        if ("CANCELLED".equals(intent.getStatus()) || "EXPIRED".equals(intent.getStatus())) {
            throw new IllegalStateException("Giao dịch đã bị hủy hoặc hết hạn, không thể xác nhận");
        }
        if (req.actualAmount() < intent.getAmount()) {
            intent.recordGatewayPayment(req.actualAmount(), Instant.now());
            webhookProcessor.publishPaymentEvent(intent, "PAYMENT_UNDERPAID");
            return toResponse(paymentIntentRepository.save(intent));
        }
        intent.markPaid(req.actualAmount());
        PaymentIntent saved = paymentIntentRepository.save(intent);
        ledgerPoster.postLedgerForPayment(saved, req.actualAmount(), "MANUAL-" + req.bankTxCode().trim());
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
                .findFirst()
                .map(this::toResponse);
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
                .filter(i -> "OVERPAID".equals(i.getStatus())
                        || "MISMATCH".equals(i.getStatus())
                        || ("EXPIRED".equals(i.getStatus()) && i.getPaidAmount() > 0)
                        || "PARTIAL_PAID".equals(i.getStatus()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentDto.AdminIntentResponse> getAllAdminIntents(int limit) {
        int cap = Math.max(1, Math.min(limit, 500));
        return paymentIntentRepository.findAll().stream()
                .sorted(Comparator.comparing(PaymentIntent::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(cap)
                .map(i -> new PaymentDto.AdminIntentResponse(
                        i.getId(), i.getPaymentCode(), i.getPayerId(),
                        i.getOrderType(), i.getOrderId(), i.getAmount(),
                        i.getPaidAmount(), i.getStatus(), i.getExpiresAt(), i.getCreatedAt()))
                .toList();
    }

    @Transactional
    public PaymentDto.IntentResponse simulateSandboxWebhook(String paymentCode, long amount, String actorId, boolean isAdmin) {
        if (!isAdmin) {
            throw new WebhookSecurityException(403, "Chức năng giả lập chỉ dành cho Quản trị viên hệ thống");
        }
        PaymentIntent intent = paymentIntentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay ma thanh toan: " + paymentCode));
        long payAmount = amount > 0 ? amount : intent.getAmount();
        long ts = Instant.now().getEpochSecond();
        String rawBody = String.format("{\"paymentCode\":\"%s\",\"amount\":%d,\"status\":\"SUCCESS\",\"timestamp\":%d}", paymentCode, payAmount, ts);
        String sig = verifier.computeHmacSha256(rawBody + "." + ts, verifier.getWebhookSecret());
        processWebhook(rawBody, sig, ts, "127.0.0.1");
        return paymentIntentRepository.findByPaymentCode(paymentCode).map(this::toResponse).orElseThrow();
    }

    @Scheduled(fixedDelayString = "${fintech.gateway.expire-poll-ms:60000}")
    @Transactional
    public int expireOverdueIntents() {
        Instant now = Instant.now();
        int expiredCount = 0;
        for (PaymentIntent intent : paymentIntentRepository.findPendingExpiredBefore(now)) {
            if (intent.markExpiredIfPast(now)) {
                paymentIntentRepository.save(intent);
                expiredCount++;
            }
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
    public boolean processWebhook(String rawBody, String signature, long timestamp) {
        return processWebhook(rawBody, signature, timestamp, "127.0.0.1");
    }

    @Transactional
    public boolean processWebhook(String rawBody, String signature, long timestamp, String sourceIp) {
        return webhookProcessor.processWebhook(rawBody, signature, timestamp, sourceIp);
    }

    private PaymentDto.IntentResponse toResponse(PaymentIntent i) {
        return new PaymentDto.IntentResponse(i.getId(), i.getPaymentCode(), i.getOrderType(),
                i.getOrderId(), i.getAmount(), i.getPaidAmount(), i.getQrUrl(), i.getStatus(), i.getExpiresAt());
    }
}
