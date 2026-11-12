package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.RoomImage;
import vn.cozynest.listing.domain.repository.RoomImageRepository;
import vn.cozynest.listing.infrastructure.persistence.mapper.RoomPersistenceMapper;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataRoomImageRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoomImageRepositoryAdapter implements RoomImageRepository {

    private final SpringDataRoomImageRepository springRepo;
    private final RoomPersistenceMapper mapper;

    @Override
    public List<RoomImage> findAll() {
        return springRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<RoomImage> findByRoomIdOrderBySortOrderAsc(Long roomId) {
        return springRepo.findByRoomIdOrderBySortOrderAsc(roomId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<RoomImage> findByRoomIdAndCoverTrue(Long roomId) {
        return springRepo.findByRoomIdAndCoverTrue(roomId).map(mapper::toDomain);
    }

    @Override
    public Optional<RoomImage> findByIdAndRoomId(Long id, Long roomId) {
        return springRepo.findByIdAndRoomId(id, roomId).map(mapper::toDomain);
    }

    @Override
    public long countByRoomId(Long roomId) {
        return springRepo.countByRoomId(roomId);
    }

    @Override
    public void resetCoverForRoom(Long roomId) {
        springRepo.resetCoverForRoom(roomId);
    }

    @Override
    public void setCoverForRoom(Long roomId, Long imageId) {
        springRepo.setCoverForRoom(roomId, imageId);
    }

    @Override
    public RoomImage save(RoomImage image) {
        return mapper.toDomain(springRepo.save(mapper.toEntity(image)));
    }

    @Override
    public void delete(RoomImage image) {
        springRepo.delete(mapper.toEntity(image));
    }
}
