package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.RoomStatusHistory;
import vn.cozynest.listing.domain.repository.RoomStatusHistoryRepository;
import vn.cozynest.listing.infrastructure.persistence.mapper.RoomPersistenceMapper;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataRoomStatusHistoryRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RoomStatusHistoryRepositoryAdapter implements RoomStatusHistoryRepository {

    private final SpringDataRoomStatusHistoryRepository springRepo;
    private final RoomPersistenceMapper mapper;

    @Override
    public List<RoomStatusHistory> findByRoomIdOrderByCreatedAtDesc(Long roomId) {
        return springRepo.findByRoomIdOrderByCreatedAtDesc(roomId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public RoomStatusHistory save(RoomStatusHistory history) {
        return mapper.toDomain(springRepo.save(mapper.toEntity(history)));
    }
}

