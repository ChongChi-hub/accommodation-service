package vn.cozynest.listing.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.listing.domain.model.LandlordSnapshot;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LandlordSnapshotRepository extends JpaRepository<LandlordSnapshot, Long> {
    Optional<LandlordSnapshot> findByLandlordId(Long landlordId);
    List<LandlordSnapshot> findByLandlordIdIn(Collection<Long> landlordIds);
}
