package vn.cozynest.listing.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.RoomImage;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {

    List<RoomImage> findByRoomIdOrderBySortOrderAsc(Long roomId);

    Optional<RoomImage> findByRoomIdAndCoverTrue(Long roomId);

    Optional<RoomImage> findByIdAndRoomId(Long id, Long roomId);

    long countByRoomId(Long roomId);

    @Modifying
    @Query("UPDATE RoomImage ri SET ri.cover = false, ri.primary = false WHERE ri.roomId = :roomId")
    void resetCoverForRoom(@Param("roomId") Long roomId);

    @Modifying
    @Query("UPDATE RoomImage ri SET ri.cover = true, ri.primary = true WHERE ri.id = :imageId AND ri.roomId = :roomId")
    void setCoverForRoom(@Param("roomId") Long roomId, @Param("imageId") Long imageId);
}

