package vn.cozynest.admin.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.admin.domain.model.DeadLetterMessage;
import vn.cozynest.admin.domain.repository.DeadLetterRepository;
import vn.cozynest.common.events.DeadLetterEnvelope;
import vn.cozynest.common.events.DomainEventEnvelope;
import vn.cozynest.common.events.EventEnvelopeCodec;
import vn.cozynest.common.events.EventTopology;

import java.time.Instant;
import java.util.UUID;

@Component
public class AdminDlqListener {

    private static final Logger log = LoggerFactory.getLogger(AdminDlqListener.class);
    private final DeadLetterRepository deadLetterRepository;

    public AdminDlqListener(DeadLetterRepository deadLetterRepository) {
        this.deadLetterRepository = deadLetterRepository;
    }

    @RabbitListener(queues = EventTopology.DLQ_QUEUE)
    @Transactional
    public void onDeadLetter(String rawDlqJson) {
        DeadLetterEnvelope dlq;
        try {
            dlq = EventEnvelopeCodec.decodeDeadLetter(rawDlqJson);
        } catch (Exception ex) {
            log.warn("Unparseable DLQ payload quarantined safely: {}", ex.getMessage());
            String syntheticEventId = UUID.randomUUID().toString();
            deadLetterRepository.save(new DeadLetterMessage(
                    UUID.randomUUID().toString(), syntheticEventId, "MALFORMED_DLQ_EVENT", 1,
                    "UNKNOWN", "UNKNOWN", "unknown",
                    EventTopology.sanitizePiiForDlq("{\"rawTruncated\":true}"),
                    ex.getMessage(), 1, "PENDING_REPLAY", null, null, Instant.now()
            ));
            return;
        }

        DomainEventEnvelope env = dlq.envelope();
        if (deadLetterRepository.findByEventId(env.eventId()).isPresent()) {
            return;
        }
        String sanitizedPayload = EventTopology.sanitizePiiForDlq(env.payloadJson());
        deadLetterRepository.save(new DeadLetterMessage(
                UUID.randomUUID().toString(),
                env.eventId(),
                env.eventType(),
                env.schemaVersion(),
                env.aggregateType(),
                env.aggregateId(),
                dlq.sourceService(),
                sanitizedPayload,
                dlq.failureReason(),
                dlq.retryCount(),
                "PENDING_REPLAY",
                null,
                null,
                Instant.now()
        ));
    }
}

