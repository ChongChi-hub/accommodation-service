package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.FaceEventOutbox;
import vn.cozynest.identity.domain.repository.FaceEventOutboxRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.FaceEventOutboxJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataFaceEventOutboxRepository;

import java.util.List;

@Component
public class FaceEventOutboxRepositoryAdapter implements FaceEventOutboxRepository {

    private final SpringDataFaceEventOutboxRepository repository;

    public FaceEventOutboxRepositoryAdapter(SpringDataFaceEventOutboxRepository repository) {
        this.repository = repository;
    }

    @Override
    public FaceEventOutbox save(FaceEventOutbox outbox) {
        FaceEventOutboxJpaEntity entity = new FaceEventOutboxJpaEntity();
        entity.setId(outbox.getId());
        entity.setAggregateId(outbox.getAggregateId());
        entity.setEventType(outbox.getEventType());
        entity.setPayload(outbox.getPayload());
        entity.setPublished(outbox.isPublished());
        FaceEventOutboxJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<FaceEventOutbox> findUnpublished(int limit) {
        return repository.findUnpublished(PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    private FaceEventOutbox toDomain(FaceEventOutboxJpaEntity e) {
        return new FaceEventOutbox(
                e.getId(), e.getAggregateId(), e.getEventType(),
                e.getPayload(), e.isPublished(), e.getCreatedAt()
        );
    }
}

