package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.LandlordSnapshot;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LandlordSnapshotRepository {
    Optional<LandlordSnapshot> findByLandlordId(Long landlordId);
    List<LandlordSnapshot> findByLandlordIdIn(Collection<Long> landlordIds);
    default List<LandlordSnapshot> findAll() {
        return java.util.Collections.emptyList();
    }
    LandlordSnapshot save(LandlordSnapshot snapshot);
}
