package vn.cozynest.common.events;

import java.time.Instant;
import java.util.concurrent.Callable;

/**
 * Idempotent & monotonic inbox guard preventing duplicate or out-of-order stale domain event application.
 */
public final class EventInboxProcessor {

    public interface InboxStore {
        boolean existsByEventId(String eventId);

        Instant findLastOccurredAt(String aggregateType, String aggregateId, String eventType);

        void recordProcessed(DomainEventEnvelope envelope);

        void recordDeadLetter(DomainEventEnvelope envelope, String reason);
    }

    private EventInboxProcessor() {
    }

    public static ProcessOutcome processIdempotently(
            InboxStore store,
            DomainEventEnvelope envelope,
            Callable<Void> handler
    ) {
        if (store.existsByEventId(envelope.eventId())) {
            return ProcessOutcome.DUPLICATE_IGNORED;
        }
        Instant lastOccurred = store.findLastOccurredAt(
                envelope.aggregateType(), envelope.aggregateId(), envelope.eventType());
        if (lastOccurred != null && envelope.occurredAt().isBefore(lastOccurred)) {
            store.recordDeadLetter(envelope, "OUT_OF_ORDER_STALE_EVENT");
            return ProcessOutcome.STALE_OUT_OF_ORDER;
        }
        try {
            handler.call();
            store.recordProcessed(envelope);
            return ProcessOutcome.PROCESSED;
        } catch (Exception ex) {
            store.recordDeadLetter(envelope, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
            return ProcessOutcome.SENT_TO_DLQ;
        }
    }

    public enum ProcessOutcome {
        PROCESSED,
        DUPLICATE_IGNORED,
        STALE_OUT_OF_ORDER,
        SENT_TO_DLQ
    }
}

