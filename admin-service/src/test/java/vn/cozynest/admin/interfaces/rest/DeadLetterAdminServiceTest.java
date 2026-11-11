package vn.cozynest.admin.interfaces.rest;

import org.junit.jupiter.api.Test;
import vn.cozynest.admin.application.service.DeadLetterAdminService;
import vn.cozynest.admin.domain.model.DeadLetterMessage;
import vn.cozynest.admin.domain.repository.DeadLetterRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeadLetterAdminServiceTest {

    @Test
    void replaysPendingDeadLetterOnceAndRejectsDoubleReplay() {
        Map<String, DeadLetterMessage> store = new HashMap<>();
        DeadLetterMessage pending = new DeadLetterMessage(
                "dlq-1", "evt-100", "PaymentConfirmedEvent", 1,
                "PaymentIntent", "pi-1", "billing-service", "{}", "timeout",
                0, "PENDING_REPLAY", null, null, Instant.now());
        store.put(pending.getId(), pending);

        DeadLetterRepository repo = new DeadLetterRepository() {
            @Override public DeadLetterMessage save(DeadLetterMessage msg) { store.put(msg.getId(), msg); return msg; }
            @Override public Optional<DeadLetterMessage> findById(String id) { return Optional.ofNullable(store.get(id)); }
            @Override public List<DeadLetterMessage> findAll() { return List.copyOf(store.values()); }
        };

        DeadLetterAdminService service = new DeadLetterAdminService(repo);
        DeadLetterMessage replayed = service.replay("dlq-1", "admin-1");
        assertThat(replayed.getStatus()).isEqualTo("REPLAYED");
        assertThat(replayed.getRetryCount()).isEqualTo(1);
        assertThat(replayed.getReplayedBy()).isEqualTo("admin-1");

        assertThatThrownBy(() -> service.replay("dlq-1", "admin-1"))
                .isInstanceOf(IllegalStateException.class);
    }
}

