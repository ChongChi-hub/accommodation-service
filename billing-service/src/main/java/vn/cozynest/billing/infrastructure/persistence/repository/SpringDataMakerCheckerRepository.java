package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.MakerCheckerJpaEntity;

import java.util.List;

public interface SpringDataMakerCheckerRepository extends JpaRepository<MakerCheckerJpaEntity, String> {
    List<MakerCheckerJpaEntity> findByStatusOrderByCreatedAtDesc(String status);
}

