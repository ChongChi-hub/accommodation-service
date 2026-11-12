package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.Amenity;
import vn.cozynest.listing.domain.repository.AmenityRepository;
import vn.cozynest.listing.infrastructure.persistence.mapper.RoomPersistenceMapper;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataAmenityRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AmenityRepositoryAdapter implements AmenityRepository {

    private final SpringDataAmenityRepository springRepo;
    private final RoomPersistenceMapper mapper;

    @Override
    public List<Amenity> findAll() {
        return springRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Amenity> findAllById(Iterable<Long> ids) {
        return springRepo.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Amenity> findById(Long id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Amenity save(Amenity amenity) {
        return mapper.toDomain(springRepo.save(mapper.toEntity(amenity)));
    }
}
