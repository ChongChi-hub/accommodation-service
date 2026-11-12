package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.AmenityJpaEntity;

public interface SpringDataAmenityRepository extends JpaRepository<AmenityJpaEntity, Long> {
}

