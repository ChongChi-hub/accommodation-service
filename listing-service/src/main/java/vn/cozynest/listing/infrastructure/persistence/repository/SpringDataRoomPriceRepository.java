package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.RoomPriceJpaEntity;

import java.util.List;

public interface SpringDataRoomPriceRepository extends JpaRepository<RoomPriceJpaEntity, Long> {
    List<RoomPriceJpaEntity> findByRoomId(Long roomId);
    void deleteByRoomId(Long roomId);
}
