package vn.cozynest.listing.infrastructure.persistence.adapter;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import vn.cozynest.listing.domain.model.ListingEventOutbox;
import vn.cozynest.listing.domain.repository.ListingEventOutboxRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.ListingEventOutboxJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataListingEventOutboxRepository;

import java.util.List;
import java.util.Optional;

@Component
public class ListingEventOutboxRepositoryAdapter implements ListingEventOutboxRepository {

    private final SpringDataListingEventOutboxRepository repository;

    public ListingEventOutboxRepositoryAdapter(SpringDataListingEventOutboxRepository repository) {
        this.repository = repository;
    }

    @Override
    public ListingEventOutbox save(ListingEventOutbox outbox) {
        ListingEventOutboxJpaEntity entity = toEntity(outbox);
        ListingEventOutboxJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<ListingEventOutbox> findPending(int limit) {
        return repository.findPending(PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ListingEventOutbox> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    private ListingEventOutbox toDomain(ListingEventOutboxJpaEntity entity) {
        return new ListingEventOutbox(
                entity.getId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getRetryCount(),
                entity.getCreatedAt(),
                entity.getProcessedAt()
        );
    }

    private ListingEventOutboxJpaEntity toEntity(ListingEventOutbox domain) {
        return ListingEventOutboxJpaEntity.builder()
                .id(domain.getId())
                .aggregateType(domain.getAggregateType())
                .aggregateId(domain.getAggregateId())
                .eventType(domain.getEventType())
                .payload(domain.getPayload())
                .status(domain.getStatus())
                .retryCount(domain.getRetryCount())
                .createdAt(domain.getCreatedAt())
                .processedAt(domain.getProcessedAt())
                .build();
    }
}
