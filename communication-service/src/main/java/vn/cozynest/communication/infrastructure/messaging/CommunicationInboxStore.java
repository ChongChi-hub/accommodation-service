package vn.cozynest.communication.infrastructure.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import vn.cozynest.common.events.DeadLetterEnvelope;
import vn.cozynest.common.events.DomainEventEnvelope;
import vn.cozynest.common.events.EventEnvelopeCodec;
import vn.cozynest.common.events.EventInboxProcessor;
import vn.cozynest.common.events.EventTopology;
import vn.cozynest.communication.infrastructure.persistence.EventInboxJpaEntity;
import vn.cozynest.communication.infrastructure.persistence.SpringDataEventInboxRepository;

import java.time.Instant;
import java.util.Optional;

@Component
public class CommunicationInboxStore implements EventInboxProcessor.InboxStore {

    private final SpringDataEventInboxRepository inboxRepository;
    private final Optional<RabbitTemplate> rabbitTemplate;

    public CommunicationInboxStore(SpringDataEventInboxRepository inboxRepository,
                                   Optional<RabbitTemplate> rabbitTemplate) {
        this.inboxRepository = inboxRepository;
        this.rabbitTemplate = rabbitTemplate != null ? rabbitTemplate : Optional.empty();
    }

    @Override
    public boolean existsByEventId(String eventId) {
        return inboxRepository.existsById(eventId);
    }

    @Override
    public Instant findLastOccurredAt(String aggregateType, String aggregateId, String eventType) {
        return inboxRepository.findMaxOccurredAt(aggregateType, aggregateId, eventType).orElse(null);
    }

    @Override
    public void recordProcessed(DomainEventEnvelope envelope) {
        inboxRepository.save(new EventInboxJpaEntity(
                envelope.eventId(),
                envelope.eventType(),
                envelope.schemaVersion(),
                envelope.aggregateType(),
                envelope.aggregateId(),
                envelope.occurredAt()
        ));
    }

    @Override
    public void recordDeadLetter(DomainEventEnvelope envelope, String reason) {
        if (rabbitTemplate.isEmpty()) {
            return;
        }
        DomainEventEnvelope sanitizedEnvelope = new DomainEventEnvelope(
                envelope.eventId(),
                envelope.eventType(),
                envelope.schemaVersion(),
                envelope.aggregateType(),
                envelope.aggregateId(),
                envelope.correlationId(),
                envelope.occurredAt(),
                EventTopology.sanitizePiiForDlq(envelope.payloadJson())
        );
        DeadLetterEnvelope dlq = new DeadLetterEnvelope(
                sanitizedEnvelope,
                "communication-service",
                reason != null ? reason : "PROCESSING_FAILED",
                1
        );
        rabbitTemplate.get().convertAndSend(
                EventTopology.DLX_EXCHANGE,
                EventTopology.DLQ_ROUTING_KEY,
                EventEnvelopeCodec.encodeDeadLetter(dlq)
        );
    }
}

