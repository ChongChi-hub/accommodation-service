package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.cozynest.listing.infrastructure.persistence.entity.RoomImageJpaEntity;

import java.util.List;
import java.util.Optional;

public interface SpringDataRoomImageRepository extends JpaRepository<RoomImageJpaEntity, Long> {
    List<RoomImageJpaEntity> findByRoomIdOrderBySortOrderAsc(Long roomId);
    Optional<RoomImageJpaEntity> findByRoomIdAndCoverTrue(Long roomId);
    Optional<RoomImageJpaEntity> findByIdAndRoomId(Long id, Long roomId);
    long countByRoomId(Long roomId);

    @Modifying
    @Query("UPDATE RoomImageJpaEntity ri SET ri.cover = false, ri.primary = false WHERE ri.roomId = :roomId")
    void resetCoverForRoom(@Param("roomId") Long roomId);

    @Modifying
    @Query("UPDATE RoomImageJpaEntity ri SET ri.cover = true, ri.primary = true WHERE ri.id = :imageId AND ri.roomId = :roomId")
    void setCoverForRoom(@Param("roomId") Long roomId, @Param("imageId") Long imageId);
}

