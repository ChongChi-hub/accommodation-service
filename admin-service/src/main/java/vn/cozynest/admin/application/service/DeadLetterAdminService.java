package vn.cozynest.admin.application.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.admin.domain.model.DeadLetterMessage;
import vn.cozynest.admin.domain.repository.DeadLetterRepository;
import vn.cozynest.common.events.DomainEventEnvelope;
import vn.cozynest.common.events.EventEnvelopeCodec;
import vn.cozynest.common.events.EventTopology;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeadLetterAdminService {

    private final DeadLetterRepository repository;
    private final Optional<RabbitTemplate> rabbitTemplate;

    public DeadLetterAdminService(DeadLetterRepository repository) {
        this(repository, Optional.empty());
    }

    @Autowired
    public DeadLetterAdminService(DeadLetterRepository repository, Optional<RabbitTemplate> rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate != null ? rabbitTemplate : Optional.empty();
    }

    @Transactional(readOnly = true)
    public List<DeadLetterMessage> listAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getMetrics() {
        List<DeadLetterMessage> all = repository.findAll();
        long pending = all.stream().filter(m -> "PENDING_REPLAY".equals(m.getStatus())).count();
        long replayed = all.stream().filter(m -> "REPLAYED".equals(m.getStatus())).count();
        long discarded = all.stream().filter(m -> "DISCARDED".equals(m.getStatus())).count();
        return Map.of(
                "total", (long) all.size(),
                "pendingReplay", pending,
                "replayed", replayed,
                "discarded", discarded
        );
    }

    @Transactional
    public DeadLetterMessage replay(String id, String adminId) {
        DeadLetterMessage msg = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay ban ghi DLQ: " + id));
        msg.markReplayed(adminId);
        DeadLetterMessage saved = repository.save(msg);

        if (rabbitTemplate.isPresent()) {
            DomainEventEnvelope replayedEnvelope = new DomainEventEnvelope(
                    UUID.randomUUID().toString(),
                    msg.getEventType(),
                    msg.getSchemaVersion(),
                    msg.getAggregateType(),
                    msg.getAggregateId(),
                    msg.getEventId(),
                    Instant.now(),
                    msg.getPayloadJson()
            );
            String prefix = resolveRoutingPrefix(msg.getSourceService());
            String routingKey = prefix + "." + msg.getEventType().toLowerCase().replace('_', '.');
            rabbitTemplate.get().convertAndSend(
                    EventTopology.EVENTS_EXCHANGE,
                    routingKey,
                    EventEnvelopeCodec.encode(replayedEnvelope)
            );
        }
        return saved;
    }

    @Transactional
    public DeadLetterMessage discard(String id, String adminId) {
        DeadLetterMessage msg = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay ban ghi DLQ: " + id));
        DeadLetterMessage updated = new DeadLetterMessage(
                msg.getId(), msg.getEventId(), msg.getEventType(), msg.getSchemaVersion(),
                msg.getAggregateType(), msg.getAggregateId(), msg.getSourceService(),
                msg.getPayloadJson(), msg.getFailureReason(), msg.getRetryCount(),
                "DISCARDED", adminId, Instant.now(), msg.getCreatedAt()
        );
        return repository.save(updated);
    }

    private String resolveRoutingPrefix(String sourceService) {
        if (sourceService != null && sourceService.contains("billing")) {
            return "billing";
        }
        if (sourceService != null && sourceService.contains("listing")) {
            return "listing";
        }
        return "booking";
    }
}
