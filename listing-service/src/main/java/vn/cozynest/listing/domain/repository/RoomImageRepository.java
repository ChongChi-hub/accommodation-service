package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.RoomImage;

import java.util.List;
import java.util.Optional;

public interface RoomImageRepository {
    List<RoomImage> findAll();
    List<RoomImage> findByRoomIdOrderBySortOrderAsc(Long roomId);
    Optional<RoomImage> findByRoomIdAndCoverTrue(Long roomId);
    Optional<RoomImage> findByIdAndRoomId(Long id, Long roomId);
    long countByRoomId(Long roomId);
    void resetCoverForRoom(Long roomId);
    void setCoverForRoom(Long roomId, Long imageId);
    RoomImage save(RoomImage image);
    void delete(RoomImage image);
}
