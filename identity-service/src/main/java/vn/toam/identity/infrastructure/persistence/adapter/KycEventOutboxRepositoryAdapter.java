package vn.toam.identity.infrastructure.persistence.adapter;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import vn.toam.identity.domain.model.KycEventOutbox;
import vn.toam.identity.domain.repository.KycEventOutboxRepository;
import vn.toam.identity.infrastructure.persistence.entity.KycEventOutboxJpaEntity;
import vn.toam.identity.infrastructure.persistence.repository.SpringDataKycEventOutboxRepository;

import java.util.List;

@Component
public class KycEventOutboxRepositoryAdapter implements KycEventOutboxRepository {

    private final SpringDataKycEventOutboxRepository springRepo;

    public KycEventOutboxRepositoryAdapter(SpringDataKycEventOutboxRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public KycEventOutbox save(KycEventOutbox outbox) {
        KycEventOutboxJpaEntity entity = toEntity(outbox);
        KycEventOutboxJpaEntity saved = springRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<KycEventOutbox> findPending(int limit) {
        return springRepo.findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    private KycEventOutbox toDomain(KycEventOutboxJpaEntity entity) {
        return new KycEventOutbox(
                entity.getId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getProcessedAt()
        );
    }

    private KycEventOutboxJpaEntity toEntity(KycEventOutbox domain) {
        KycEventOutboxJpaEntity entity = new KycEventOutboxJpaEntity();
        entity.setId(domain.getId());
        entity.setAggregateType(domain.getAggregateType());
        entity.setAggregateId(domain.getAggregateId());
        entity.setEventType(domain.getEventType());
        entity.setPayload(domain.getPayload());
        entity.setStatus(domain.getStatus());
        entity.setProcessedAt(domain.getProcessedAt());
        return entity;
    }
}

