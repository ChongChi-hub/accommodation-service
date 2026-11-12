package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.RoomStatusHistoryJpaEntity;

import java.util.List;

public interface SpringDataRoomStatusHistoryRepository extends JpaRepository<RoomStatusHistoryJpaEntity, Long> {
    List<RoomStatusHistoryJpaEntity> findByRoomIdOrderByCreatedAtDesc(Long roomId);
}

