package vn.toam.listing.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.toam.listing.domain.model.RoomStatusHistory;

import java.util.List;

@Repository
public interface RoomStatusHistoryRepository extends JpaRepository<RoomStatusHistory, Long> {

    List<RoomStatusHistory> findByRoomIdOrderByCreatedAtDesc(Long roomId);
}

