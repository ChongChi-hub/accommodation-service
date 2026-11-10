package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.RiskProfileJpaEntity;

public interface SpringDataRiskProfileRepository extends JpaRepository<RiskProfileJpaEntity, String> {
}

