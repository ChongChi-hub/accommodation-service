package vn.cozynest.admin.interfaces.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import vn.cozynest.admin.application.service.DeadLetterAdminService;
import vn.cozynest.admin.domain.model.DeadLetterMessage;
import vn.cozynest.admin.domain.repository.DeadLetterRepository;
import vn.cozynest.common.events.EventTopology;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeadLetterAdminServiceTest {

    @Test
    @DisplayName("Replays pending dead letter once and rejects double replay or replaying discarded")
    void replaysPendingDeadLetterOnceAndRejectsDoubleReplay() {
        Map<String, DeadLetterMessage> store = new HashMap<>();
        DeadLetterMessage pending = new DeadLetterMessage(
                "dlq-1", "evt-100", "PaymentConfirmedEvent", 1,
                "PaymentIntent", "pi-1", "billing-service", "{}", "timeout",
                0, "PENDING_REPLAY", null, null, Instant.now());
        store.put(pending.getId(), pending);

        DeadLetterRepository repo = createInMemoryRepo(store);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        DeadLetterAdminService service = new DeadLetterAdminService(repo, Optional.of(rabbit));

        DeadLetterMessage replayed = service.replay("dlq-1", "admin-1");
        assertThat(replayed.getStatus()).isEqualTo("REPLAYED");
        assertThat(replayed.getRetryCount()).isEqualTo(1);
        assertThat(replayed.getReplayedBy()).isEqualTo("admin-1");
        verify(rabbit).convertAndSend(
                eq(EventTopology.EVENTS_EXCHANGE),
                eq("billing.paymentconfirmedevent"),
                anyString()
        );

        assertThatThrownBy(() -> service.replay("dlq-1", "admin-1"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Discards dead letter message, updates status to DISCARDED and prevents replay")
    void discardsDeadLetterAndBlocksReplay() {
        Map<String, DeadLetterMessage> store = new HashMap<>();
        DeadLetterMessage pending = new DeadLetterMessage(
                "dlq-2", "evt-101", "KYC_SUBMITTED", 1,
                "KYC_VERIFICATION", "usr-1", "identity-service", "{}", "bad payload",
                0, "PENDING_REPLAY", null, null, Instant.now());
        store.put(pending.getId(), pending);

        DeadLetterRepository repo = createInMemoryRepo(store);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        DeadLetterAdminService service = new DeadLetterAdminService(repo, Optional.of(rabbit));

        DeadLetterMessage discarded = service.discard("dlq-2", "admin-2");
        assertThat(discarded.getStatus()).isEqualTo("DISCARDED");

        assertThatThrownBy(() -> service.replay("dlq-2", "admin-2"))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> service.discard("dlq-2", "admin-2"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Calculates accurate DLQ metrics across status breakdowns")
    void calculatesMetricsAccurately() {
        Map<String, DeadLetterMessage> store = new HashMap<>();
        store.put("1", new DeadLetterMessage("1", "e1", "E", 1, "A", "a", "s", "{}", "err", 0, "PENDING_REPLAY", null, null, Instant.now()));
        store.put("2", new DeadLetterMessage("2", "e2", "E", 1, "A", "a", "s", "{}", "err", 1, "REPLAYED", "adm", Instant.now(), Instant.now()));
        store.put("3", new DeadLetterMessage("3", "e3", "E", 1, "A", "a", "s", "{}", "err", 0, "DISCARDED", "adm", Instant.now(), Instant.now()));

        DeadLetterRepository repo = createInMemoryRepo(store);
        DeadLetterAdminService service = new DeadLetterAdminService(repo, Optional.empty());

        Map<String, Long> metrics = service.getMetrics();
        assertThat(metrics.get("total")).isEqualTo(3L);
        assertThat(metrics.get("pendingReplay")).isEqualTo(1L);
        assertThat(metrics.get("replayed")).isEqualTo(1L);
        assertThat(metrics.get("discarded")).isEqualTo(1L);
    }

    private DeadLetterRepository createInMemoryRepo(Map<String, DeadLetterMessage> store) {
        return new DeadLetterRepository() {
            @Override public DeadLetterMessage save(DeadLetterMessage msg) { store.put(msg.getId(), msg); return msg; }
            @Override public Optional<DeadLetterMessage> findById(String id) { return Optional.ofNullable(store.get(id)); }
            @Override public Optional<DeadLetterMessage> findByEventId(String eventId) {
                return store.values().stream().filter(m -> eventId.equals(m.getEventId())).findFirst();
            }
            @Override public List<DeadLetterMessage> findAll() { return List.copyOf(store.values()); }
        };
    }
}
