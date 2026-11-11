package vn.cozynest.admin.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.admin.domain.model.DeadLetterMessage;
import vn.cozynest.admin.domain.repository.DeadLetterRepository;
import vn.cozynest.admin.infrastructure.persistence.entity.DeadLetterMessageJpaEntity;
import vn.cozynest.admin.infrastructure.persistence.repository.SpringDataDeadLetterRepository;

import java.util.List;
import java.util.Optional;

@Component
public class DeadLetterRepositoryAdapter implements DeadLetterRepository {

    private final SpringDataDeadLetterRepository repository;

    public DeadLetterRepositoryAdapter(SpringDataDeadLetterRepository repository) {
        this.repository = repository;
    }

    @Override
    public DeadLetterMessage save(DeadLetterMessage msg) {
        DeadLetterMessageJpaEntity saved = repository.save(DeadLetterMessageJpaEntity.builder()
                .id(msg.getId())
                .eventId(msg.getEventId())
                .eventType(msg.getEventType())
                .schemaVersion(msg.getSchemaVersion())
                .aggregateType(msg.getAggregateType())
                .aggregateId(msg.getAggregateId())
                .sourceService(msg.getSourceService())
                .payloadJson(msg.getPayloadJson())
                .failureReason(msg.getFailureReason())
                .retryCount(msg.getRetryCount())
                .status(msg.getStatus())
                .replayedBy(msg.getReplayedBy())
                .replayedAt(msg.getReplayedAt())
                .createdAt(msg.getCreatedAt())
                .build());
        return toDomain(saved);
    }

    @Override
    public Optional<DeadLetterMessage> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<DeadLetterMessage> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toDomain).toList();
    }

    private DeadLetterMessage toDomain(DeadLetterMessageJpaEntity e) {
        return new DeadLetterMessage(
                e.getId(), e.getEventId(), e.getEventType(), e.getSchemaVersion(),
                e.getAggregateType(), e.getAggregateId(), e.getSourceService(),
                e.getPayloadJson(), e.getFailureReason(), e.getRetryCount(),
                e.getStatus(), e.getReplayedBy(), e.getReplayedAt(), e.getCreatedAt()
        );
    }
}

