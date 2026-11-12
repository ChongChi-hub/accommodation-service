package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.LandlordSnapshot;
import vn.cozynest.listing.domain.repository.LandlordSnapshotRepository;
import vn.cozynest.listing.infrastructure.persistence.mapper.ListingCatalogPersistenceMapper;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataLandlordSnapshotRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LandlordSnapshotRepositoryAdapter implements LandlordSnapshotRepository {

    private final SpringDataLandlordSnapshotRepository springRepo;
    private final ListingCatalogPersistenceMapper mapper;

    @Override
    public Optional<LandlordSnapshot> findByLandlordId(Long landlordId) {
        return springRepo.findByLandlordId(landlordId).map(mapper::toDomain);
    }

    @Override
    public List<LandlordSnapshot> findByLandlordIdIn(Collection<Long> landlordIds) {
        return springRepo.findByLandlordIdIn(landlordIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    public LandlordSnapshot save(LandlordSnapshot snapshot) {
        return mapper.toDomain(springRepo.save(mapper.toEntity(snapshot)));
    }
}
