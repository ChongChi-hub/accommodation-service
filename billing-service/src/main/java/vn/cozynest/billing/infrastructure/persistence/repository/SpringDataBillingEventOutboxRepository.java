package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.BillingEventOutboxJpaEntity;

import java.util.List;

public interface SpringDataBillingEventOutboxRepository extends JpaRepository<BillingEventOutboxJpaEntity, String> {
    List<BillingEventOutboxJpaEntity> findTop50ByStatusOrderByCreatedAtAsc(String status);
}

