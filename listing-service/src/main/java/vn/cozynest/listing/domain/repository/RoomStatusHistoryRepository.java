package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.RoomStatusHistory;

import java.util.List;

public interface RoomStatusHistoryRepository {
    List<RoomStatusHistory> findByRoomIdOrderByCreatedAtDesc(Long roomId);
    RoomStatusHistory save(RoomStatusHistory history);
}
