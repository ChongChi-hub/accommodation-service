package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.ConsentHistoryJpaEntity;

import java.util.List;

public interface SpringDataConsentHistoryRepository extends JpaRepository<ConsentHistoryJpaEntity, String> {
    List<ConsentHistoryJpaEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}
