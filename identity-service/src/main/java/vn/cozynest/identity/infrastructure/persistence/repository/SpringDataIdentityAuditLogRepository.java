package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.identity.infrastructure.persistence.entity.IdentityAuditLogJpaEntity;

import java.util.List;

@Repository
public interface SpringDataIdentityAuditLogRepository extends JpaRepository<IdentityAuditLogJpaEntity, String> {
    List<IdentityAuditLogJpaEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}
