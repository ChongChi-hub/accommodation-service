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

    Optional<Room> findBySlugAndDeletedAtIsNull(String slug);

    Optional<Room> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
        SELECT r FROM Room r
        WHERE r.provinceCode = :provinceCode
          AND r.id != :roomId
          AND r.deletedAt IS NULL
          AND r.hiddenAt IS NULL
          AND r.approved = true
          AND (r.status = 'AVAILABLE' OR r.status = 'PUBLISHED')
          AND r.price BETWEEN :minPrice AND :maxPrice
        ORDER BY r.ratingAvg DESC, r.id DESC
    """)
    List<Room> findSimilarRooms(
        @Param("provinceCode") String provinceCode,
        @Param("roomId") Long roomId,
        @Param("minPrice") java.math.BigDecimal minPrice,
        @Param("maxPrice") java.math.BigDecimal maxPrice,
        Pageable pageable
    );

    @Query("""
        SELECT p.code, p.name, p.fullName, p.slug, p.imageUrl, COUNT(r.id)
        FROM Province p
        LEFT JOIN Room r ON r.provinceCode = p.code AND r.deletedAt IS NULL AND r.hiddenAt IS NULL AND r.approved = true AND (r.status = 'AVAILABLE' OR r.status = 'PUBLISHED')
        GROUP BY p.code, p.name, p.fullName, p.slug, p.imageUrl, p.id
        ORDER BY COUNT(r.id) DESC, CASE WHEN p.slug IS NOT NULL THEN 0 ELSE 1 END, p.id ASC
    """)
    List<Object[]> findHotAreas();

    @Query("SELECT COUNT(r) FROM Room r WHERE r.deletedAt IS NULL AND r.hiddenAt IS NULL AND r.approved = true AND (r.status = 'AVAILABLE' OR r.status = 'PUBLISHED')")
    long countActiveRooms();

    @Query("SELECT COUNT(DISTINCT r.provinceCode) FROM Room r WHERE r.deletedAt IS NULL AND r.hiddenAt IS NULL AND r.approved = true AND (r.status = 'AVAILABLE' OR r.status = 'PUBLISHED')")
    long countActiveProvinces();

    @Query("SELECT COUNT(DISTINCT r.landlordId) FROM Room r WHERE r.deletedAt IS NULL AND r.hiddenAt IS NULL AND r.approved = true")
    long countVerifiedLandlords();

    @Query("""
        SELECT r.roomTypeCode, COUNT(r.id)
        FROM Room r
        WHERE r.deletedAt IS NULL AND r.hiddenAt IS NULL AND r.approved = true
          AND (r.status = 'AVAILABLE' OR r.status = 'PUBLISHED')
        GROUP BY r.roomTypeCode
    """)
    List<Object[]> countActiveByRoomTypeCode();

    @Query("""
        SELECT MIN(r.price), MAX(r.price)
        FROM Room r
        WHERE r.deletedAt IS NULL AND r.hiddenAt IS NULL AND r.approved = true
          AND (r.status = 'AVAILABLE' OR r.status = 'PUBLISHED')
    """)
    List<Object[]> findActivePriceRange();

    @Query("""
        SELECT a.id, a.code, a.name, COUNT(r.id)
        FROM Room r
        JOIN r.amenities a
        WHERE r.deletedAt IS NULL AND r.hiddenAt IS NULL AND r.approved = true
          AND (r.status = 'AVAILABLE' OR r.status = 'PUBLISHED')
        GROUP BY a.id, a.code, a.name
        ORDER BY COUNT(r.id) DESC
    """)
    List<Object[]> countActiveByAmenity();

    @Query("""
        SELECT p.code, p.name, COUNT(r.id)
        FROM Room r
        JOIN Province p ON p.code = r.provinceCode
        WHERE r.deletedAt IS NULL AND r.hiddenAt IS NULL AND r.approved = true
          AND (r.status = 'AVAILABLE' OR r.status = 'PUBLISHED')
        GROUP BY p.code, p.name
        ORDER BY COUNT(r.id) DESC
    """)
    List<Object[]> countActiveByProvince();

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
