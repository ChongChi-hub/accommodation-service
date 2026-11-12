package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.RoomType;
import vn.cozynest.listing.domain.repository.RoomTypeRepository;
import vn.cozynest.listing.infrastructure.persistence.mapper.ListingCatalogPersistenceMapper;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataRoomTypeRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoomTypeRepositoryAdapter implements RoomTypeRepository {

    private final SpringDataRoomTypeRepository springRepo;
    private final ListingCatalogPersistenceMapper mapper;

    @Override
    public List<RoomType> findAll() {
        return springRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<RoomType> findAllByOrderByIdAsc() {
        return springRepo.findAllByOrderByIdAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<RoomType> findByCodeIgnoreCase(String code) {
        return springRepo.findByCodeIgnoreCase(code).map(mapper::toDomain);
    }
}

