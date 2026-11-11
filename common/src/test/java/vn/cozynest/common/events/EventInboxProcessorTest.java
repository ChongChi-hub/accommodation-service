package vn.cozynest.common.events;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EventInboxProcessorTest {

    static class InMemoryInboxStore implements EventInboxProcessor.InboxStore {
        final Set<String> processedIds = new HashSet<>();
        final Map<String, Instant> latestByKey = new HashMap<>();
        final List<String> deadLetters = new ArrayList<>();

        @Override public boolean existsByEventId(String eventId) { return processedIds.contains(eventId); }

        @Override
        public Instant findLastOccurredAt(String aggregateType, String aggregateId, String eventType) {
            return latestByKey.get(aggregateType + ":" + aggregateId + ":" + eventType);
        }

        @Override
        public void recordProcessed(DomainEventEnvelope envelope) {
            processedIds.add(envelope.eventId());
            latestByKey.put(envelope.aggregateType() + ":" + envelope.aggregateId() + ":" + envelope.eventType(), envelope.occurredAt());
        }

        @Override
        public void recordDeadLetter(DomainEventEnvelope envelope, String reason) {
            deadLetters.add(envelope.eventId() + ":" + reason);
        }
    }

    @Test
    void ignoresDuplicatesRejectsOutOfOrderAndRoutesFailuresToDlq() {
        InMemoryInboxStore store = new InMemoryInboxStore();
        AtomicInteger handled = new AtomicInteger();

        DomainEventEnvelope newer = new DomainEventEnvelope(
                "evt-2", "ContractSignedEvent", 1, "Contract", "cnt-1", "corr-1",
                Instant.parse("2026-11-11T10:05:00Z"), "{}");

        assertThat(EventInboxProcessor.processIdempotently(store, newer, () -> { handled.incrementAndGet(); return null; }))
                .isEqualTo(EventInboxProcessor.ProcessOutcome.PROCESSED);
        assertThat(EventInboxProcessor.processIdempotently(store, newer, () -> { handled.incrementAndGet(); return null; }))
                .isEqualTo(EventInboxProcessor.ProcessOutcome.DUPLICATE_IGNORED);

        DomainEventEnvelope older = new DomainEventEnvelope(
                "evt-1", "ContractSignedEvent", 1, "Contract", "cnt-1", "corr-1",
                Instant.parse("2026-11-11T10:00:00Z"), "{}");
        assertThat(EventInboxProcessor.processIdempotently(store, older, () -> { handled.incrementAndGet(); return null; }))
                .isEqualTo(EventInboxProcessor.ProcessOutcome.STALE_OUT_OF_ORDER);

        DomainEventEnvelope broken = new DomainEventEnvelope(
                "evt-3", "PaymentConfirmedEvent", 1, "PaymentIntent", "pay-1", "corr-2",
                Instant.parse("2026-11-11T10:06:00Z"), "{}");
        assertThat(EventInboxProcessor.processIdempotently(store, broken, () -> { throw new IllegalStateException("DB timeout"); }))
                .isEqualTo(EventInboxProcessor.ProcessOutcome.SENT_TO_DLQ);

        assertThat(handled.get()).isEqualTo(1);
        assertThat(store.deadLetters).hasSize(2);
    }
}

