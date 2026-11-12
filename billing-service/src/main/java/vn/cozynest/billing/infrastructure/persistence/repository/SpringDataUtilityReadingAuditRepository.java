package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.UtilityReadingAuditJpaEntity;

import java.util.List;

public interface SpringDataUtilityReadingAuditRepository extends JpaRepository<UtilityReadingAuditJpaEntity, String> {
    List<UtilityReadingAuditJpaEntity> findByReadingIdOrderByCreatedAtDesc(String readingId);
}

