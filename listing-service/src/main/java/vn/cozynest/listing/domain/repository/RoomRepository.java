package vn.cozynest.listing.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.Room;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    List<Room> findByStatusAndApprovedTrue(String status, Pageable pageable);

    @Query("SELECT r FROM Room r WHERE r.status = 'AVAILABLE' AND r.approved = true AND r.provinceCode = :provinceCode AND r.id != :roomId ORDER BY r.ratingAvg DESC")
    List<Room> findRecommendations(@Param("provinceCode") String provinceCode, @Param("roomId") Long roomId, Pageable pageable);

    Page<Room> findByLandlordId(Long landlordId, Pageable pageable);

    Optional<Room> findByIdAndLandlordIdAndDeletedAtIsNull(Long id, Long landlordId);

    @Query("""
        SELECT r FROM Room r
        WHERE r.landlordId = :landlordId
          AND r.deletedAt IS NULL
          AND (:status IS NULL OR r.status = :status)
          AND (:search IS NULL OR LOWER(r.titleSearch) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<Room> findLandlordRooms(
        @Param("landlordId") Long landlordId,
        @Param("status") String status,
        @Param("search") String search,
        Pageable pageable
    );

    long countByLandlordIdAndDeletedAtIsNull(Long landlordId);

    long countByLandlordIdAndDeletedAtIsNullAndStatus(Long landlordId, String status);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Room r SET r.bookmarkCount = r.bookmarkCount + 1 WHERE r.id = :roomId")
    int incrementBookmarkCount(@Param("roomId") Long roomId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Room r SET r.bookmarkCount = CASE WHEN r.bookmarkCount > 0 THEN r.bookmarkCount - 1 ELSE 0 END WHERE r.id = :roomId")
    int decrementBookmarkCount(@Param("roomId") Long roomId);
}
