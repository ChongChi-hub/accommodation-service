package vn.cozynest.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Versioned domain event envelope for cross-service asynchronous delivery via outbox/inbox/DLQ (BROKER-01).
 */
public record DomainEventEnvelope(
        String eventId,
        String eventType,
        int schemaVersion,
        String aggregateType,
        String aggregateId,
        String correlationId,
        Instant occurredAt,
        String payloadJson
) {
    public static DomainEventEnvelope of(
            String eventType,
            int schemaVersion,
            String aggregateType,
            String aggregateId,
            String correlationId,
            String payloadJson
    ) {
        return new DomainEventEnvelope(
                UUID.randomUUID().toString(),
                eventType,
                schemaVersion > 0 ? schemaVersion : 1,
                aggregateType,
                aggregateId,
                correlationId,
                Instant.now(),
                payloadJson
        );
    }
}

