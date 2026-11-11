package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.TwoFactorRecoveryRequestJpaEntity;

public interface SpringDataTwoFactorRecoveryRepository extends JpaRepository<TwoFactorRecoveryRequestJpaEntity, String> {
}

