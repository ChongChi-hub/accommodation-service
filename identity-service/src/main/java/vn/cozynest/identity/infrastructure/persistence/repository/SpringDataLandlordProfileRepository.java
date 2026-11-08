package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.identity.infrastructure.persistence.entity.LandlordProfileJpaEntity;

import java.util.Optional;

@Repository
public interface SpringDataLandlordProfileRepository extends JpaRepository<LandlordProfileJpaEntity, String> {
    Optional<LandlordProfileJpaEntity> findByUserId(String userId);
    boolean existsByUserId(String userId);
}

