package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.LandlordSnapshotJpaEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataLandlordSnapshotRepository extends JpaRepository<LandlordSnapshotJpaEntity, Long> {
    Optional<LandlordSnapshotJpaEntity> findByLandlordId(Long landlordId);
    List<LandlordSnapshotJpaEntity> findByLandlordIdIn(Collection<Long> landlordIds);
}
