package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.BillingEventOutbox;
import vn.cozynest.billing.domain.repository.BillingEventOutboxRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.BillingEventOutboxJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataBillingEventOutboxRepository;

import java.util.List;

@Component
public class BillingEventOutboxRepositoryAdapter implements BillingEventOutboxRepository {
    private final SpringDataBillingEventOutboxRepository repository;

    public BillingEventOutboxRepositoryAdapter(SpringDataBillingEventOutboxRepository repository) {
        this.repository = repository;
    }

    @Override
    public BillingEventOutbox save(BillingEventOutbox event) {
        BillingEventOutboxJpaEntity saved = repository.save(BillingEventOutboxJpaEntity.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .aggregateType(event.getAggregateType())
                .aggregateId(event.getAggregateId())
                .payloadJson(event.getPayloadJson())
                .status(event.getStatus())
                .retryCount(event.getRetryCount())
                .createdAt(event.getCreatedAt())
                .publishedAt(event.getPublishedAt())
                .build());
        return new BillingEventOutbox(
                saved.getId(), saved.getEventType(), saved.getAggregateType(), saved.getAggregateId(),
                saved.getPayloadJson(), saved.getStatus(), saved.getRetryCount(),
                saved.getCreatedAt(), saved.getPublishedAt());
    }

    @Override
    public List<BillingEventOutbox> findPendingEvents() {
        return repository.findTop50ByStatusOrderByCreatedAtAsc("PENDING").stream()
                .map(e -> new BillingEventOutbox(
                        e.getId(), e.getEventType(), e.getAggregateType(), e.getAggregateId(),
                        e.getPayloadJson(), e.getStatus(), e.getRetryCount(),
                        e.getCreatedAt(), e.getPublishedAt()))
                .toList();
    }
}

