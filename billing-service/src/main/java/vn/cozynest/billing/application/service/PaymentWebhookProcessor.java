package vn.cozynest.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.cozynest.billing.application.dto.PaymentDto;
import vn.cozynest.billing.application.exception.WebhookSecurityException;
import vn.cozynest.billing.application.port.PaymentGatewayPort;
import vn.cozynest.billing.domain.model.BillingEventOutbox;
import vn.cozynest.billing.domain.model.PaymentIntent;
import vn.cozynest.billing.domain.model.PaymentWebhookLog;
import vn.cozynest.billing.domain.repository.BillingEventOutboxRepository;
import vn.cozynest.billing.domain.repository.PaymentIntentRepository;
import vn.cozynest.billing.domain.repository.PaymentWebhookLogRepository;

import java.time.Instant;

@Component
public class PaymentWebhookProcessor {
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentWebhookLogRepository webhookLogRepository;
    private final PaymentGatewayPort gatewayPort;
    private final PaymentWebhookVerifier verifier;
    private final PaymentLedgerPoster ledgerPoster;
    private final ObjectMapper objectMapper;
    private BillingEventOutboxRepository outboxRepository;

    @Autowired
    public PaymentWebhookProcessor(
            PaymentIntentRepository paymentIntentRepository,
            PaymentWebhookLogRepository webhookLogRepository,
            PaymentGatewayPort gatewayPort,
            PaymentWebhookVerifier verifier,
            PaymentLedgerPoster ledgerPoster,
            ObjectMapper objectMapper,
            @Autowired(required = false) BillingEventOutboxRepository outboxRepository) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.webhookLogRepository = webhookLogRepository;
        this.gatewayPort = gatewayPort;
        this.verifier = verifier;
        this.ledgerPoster = ledgerPoster;
        this.objectMapper = objectMapper;
        this.outboxRepository = outboxRepository;
    }

    public void setOutboxRepository(BillingEventOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    public boolean processWebhook(String rawBody, String signature, long timestamp, String sourceIp) {
        if (gatewayPort.isKillSwitchActive()) {
            logWebhook(null, sourceIp, false, "KILL_SWITCH_BLOCKED", "Payment gateway kill switch active", rawBody);
            throw new IllegalStateException("Cổng thanh toán đang tạm khóa");
        }
        if (!verifier.isIpAllowed(sourceIp)) {
            logWebhook(null, sourceIp, false, "IP_REJECTED", "IP không thuộc whitelist", rawBody);
            throw new WebhookSecurityException(403, "Webhook IP không thuộc danh sách tin cậy");
        }
        if (!verifier.isTimestampValid(timestamp, 300)) {
            logWebhook(null, sourceIp, false, "REPLAY_REJECTED", "Timestamp expired", rawBody);
            throw new IllegalArgumentException("Webhook timestamp lệch quá 5 phút");
        }
        if (!verifier.isValidSignature(rawBody + "." + timestamp, signature)) {
            logWebhook(null, sourceIp, false, "INVALID_SIGNATURE", "HMAC mismatch", rawBody);
            throw new WebhookSecurityException(401, "Chữ ký Webhook HMAC không hợp lệ");
        }

        try {
            PaymentDto.WebhookPayload payload = objectMapper.readValue(rawBody, PaymentDto.WebhookPayload.class);
            PaymentIntent intent = paymentIntentRepository.findByPaymentCode(payload.paymentCode())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã thanh toán: " + payload.paymentCode()));

            PaymentIntent.PaymentOutcome outcome = intent.recordGatewayPayment(payload.amount(), Instant.now());
            paymentIntentRepository.save(intent);

            if (outcome == PaymentIntent.PaymentOutcome.IDEMPOTENT_ALREADY_PAID) {
                logWebhook(payload.paymentCode(), sourceIp, true, "IDEMPOTENT_IGNORED", null, rawBody);
                return true;
            }
            if (outcome == PaymentIntent.PaymentOutcome.LATE_EXPIRED || outcome == PaymentIntent.PaymentOutcome.LATE_AFTER_CANCEL) {
                logWebhook(payload.paymentCode(), sourceIp, true, outcome.name(), "Late or cancelled intent", rawBody);
                return false;
            }

            ledgerPoster.postLedgerForPayment(intent, payload.amount(), null);
            publishPaymentEvent(intent, "PAYMENT_" + outcome.name());
            logWebhook(payload.paymentCode(), sourceIp, true, outcome.name(), null, rawBody);
            return true;
        } catch (IllegalArgumentException | SecurityException e) {
            logWebhook(null, sourceIp, true, "REJECTED", e.getMessage(), rawBody);
            throw e;
        } catch (Exception e) {
            logWebhook(null, sourceIp, true, "ERROR", e.getMessage(), rawBody);
            throw new RuntimeException("Xử lý webhook thanh toán thất bại", e);
        }
    }

    public void logWebhook(String paymentCode, String ip, boolean sigValid, String status, String err, String raw) {
        if (webhookLogRepository == null) return;
        webhookLogRepository.save(new PaymentWebhookLog(null, paymentCode, ip, sigValid, status, err, raw, Instant.now()));
    }

    public void publishPaymentEvent(PaymentIntent intent, String eventType) {
        if (outboxRepository == null) return;
        String payload = String.format(
                "{\"paymentCode\":\"%s\",\"orderType\":\"%s\",\"orderId\":\"%s\",\"tenantId\":\"%s\",\"paidAmount\":%d,\"status\":\"%s\"}",
                intent.getPaymentCode(), intent.getOrderType(), intent.getOrderId(),
                intent.getPayerId(), intent.getPaidAmount(), intent.getStatus()
        );
        outboxRepository.save(BillingEventOutbox.create(eventType, "PAYMENT", intent.getId(), payload));
    }
}

