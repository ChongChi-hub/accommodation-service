package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.RoomPrice;
import vn.cozynest.listing.domain.repository.RoomPriceRepository;
import vn.cozynest.listing.infrastructure.persistence.mapper.RoomPersistenceMapper;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataRoomPriceRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RoomPriceRepositoryAdapter implements RoomPriceRepository {

    private final SpringDataRoomPriceRepository springRepo;
    private final RoomPersistenceMapper mapper;

    @Override
    public List<RoomPrice> findByRoomId(Long roomId) {
        return springRepo.findByRoomId(roomId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteByRoomId(Long roomId) {
        springRepo.deleteByRoomId(roomId);
    }

    @Override
    public RoomPrice save(RoomPrice price) {
        return mapper.toDomain(springRepo.save(mapper.toEntity(price)));
    }
}

