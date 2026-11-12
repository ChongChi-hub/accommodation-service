package vn.cozynest.communication.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vn.cozynest.common.events.DeadLetterEnvelope;
import vn.cozynest.common.events.DomainEventEnvelope;
import vn.cozynest.common.events.EventEnvelopeCodec;
import vn.cozynest.common.events.EventInboxProcessor;
import vn.cozynest.common.events.EventTopology;
import vn.cozynest.communication.application.service.NotificationApplicationService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class CommunicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(CommunicationEventListener.class);
    private static final int MAX_RETRIES = 3;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CommunicationInboxStore inboxStore;
    private final NotificationApplicationService notificationService;
    private final Optional<RabbitTemplate> rabbitTemplate;

    public CommunicationEventListener(
            CommunicationInboxStore inboxStore,
            NotificationApplicationService notificationService,
            Optional<RabbitTemplate> rabbitTemplate
    ) {
        this.inboxStore = inboxStore;
        this.notificationService = notificationService;
        this.rabbitTemplate = rabbitTemplate != null ? rabbitTemplate : Optional.empty();
    }

    @RabbitListener(queues = EventTopology.COMMUNICATION_QUEUE)
    @Transactional
    public EventInboxProcessor.ProcessOutcome handleRawMessage(String rawJson) {
        DomainEventEnvelope envelope;
        try {
            envelope = EventEnvelopeCodec.decode(rawJson);
        } catch (Exception ex) {
            log.warn("Poison event payload could not be parsed; quarantining to DLQ: {}", ex.getMessage());
            publishPoisonToDlq(rawJson, "MALFORMED_ENVELOPE: " + ex.getMessage());
            return EventInboxProcessor.ProcessOutcome.SENT_TO_DLQ;
        }

        if (envelope.schemaVersion() > EventTopology.SUPPORTED_MAX_SCHEMA_VERSION) {
            inboxStore.recordDeadLetter(envelope, "UNSUPPORTED_SCHEMA_VERSION_" + envelope.schemaVersion());
            return EventInboxProcessor.ProcessOutcome.SENT_TO_DLQ;
        }

        return processWithBoundedRetry(envelope);
    }

    private EventInboxProcessor.ProcessOutcome processWithBoundedRetry(DomainEventEnvelope envelope) {
        EventInboxProcessor.ProcessOutcome lastOutcome = EventInboxProcessor.ProcessOutcome.SENT_TO_DLQ;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            lastOutcome = EventInboxProcessor.processIdempotently(inboxStore, envelope, () -> {
                dispatchNotificationFromEnvelope(envelope);
                return null;
            });
            if (lastOutcome != EventInboxProcessor.ProcessOutcome.SENT_TO_DLQ) {
                return lastOutcome;
            }
        }
        return lastOutcome;
    }

    private void dispatchNotificationFromEnvelope(DomainEventEnvelope envelope) {
        JsonNode payload = MAPPER.readTree(envelope.payloadJson() != null ? envelope.payloadJson() : "{}");
        String userId = firstNonBlank(
                payload.path("recipientId").asString(""),
                payload.path("tenantId").asString(""),
                payload.path("landlordId").asString(""),
                payload.path("userId").asString("")
        );
        if (userId.isBlank()) {
            throw new IllegalArgumentException("Missing notification recipient userId in event " + envelope.eventType());
        }
        String title = resolveTitle(envelope.eventType());
        String content = firstNonBlank(
                payload.path("message").asString(""),
                "Cập nhật " + envelope.aggregateType() + " #" + envelope.aggregateId()
        );
        String linkUrl = firstNonBlank(payload.path("linkUrl").asString(""), "/dashboard");
        notificationService.createInternalNotification(userId, envelope.aggregateType(), title, content, linkUrl);
    }

    private void publishPoisonToDlq(String rawPayload, String reason) {
        if (rabbitTemplate.isEmpty()) {
            return;
        }
        DomainEventEnvelope synthetic = new DomainEventEnvelope(
                UUID.randomUUID().toString(),
                "UNKNOWN_POISON_EVENT",
                1,
                "UNKNOWN",
                "UNKNOWN",
                "POISON",
                Instant.now(),
                EventTopology.sanitizePiiForDlq(rawPayload)
        );
        DeadLetterEnvelope dlq = new DeadLetterEnvelope(synthetic, "communication-service", reason, MAX_RETRIES);
        rabbitTemplate.get().convertAndSend(
                EventTopology.DLX_EXCHANGE,
                EventTopology.DLQ_ROUTING_KEY,
                EventEnvelopeCodec.encodeDeadLetter(dlq)
        );
    }

    private String resolveTitle(String eventType) {
        if (eventType == null) return "Thông báo hệ thống Tổ Ấm";
        return switch (eventType) {
            case "APPOINTMENT_CONFIRMED" -> "Lịch hẹn xem phòng đã được xác nhận";
            case "RENTAL_REQUEST_APPROVED" -> "Yêu cầu thuê phòng đã được chấp nhận";
            case "CONTRACT_ACTIVATED" -> "Hợp đồng thuê phòng đã có hiệu lực";
            case "PAYMENT_CONFIRMED" -> "Xác nhận thanh toán thành công";
            case "ESCROW_RELEASED" -> "Tiền đặt cọc đã được giải ngân";
            case "INVOICE_ISSUED" -> "Hóa đơn thuê phòng kỳ mới đã phát hành";
            default -> "Cập nhật nghiệp vụ: " + eventType;
        };
    }

    private String firstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                return c;
            }
        }
        return "";
    }
}

