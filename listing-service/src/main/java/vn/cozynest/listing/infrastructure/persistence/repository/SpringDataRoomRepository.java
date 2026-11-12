package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.cozynest.listing.infrastructure.persistence.entity.RoomJpaEntity;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataRoomRepository
        extends JpaRepository<RoomJpaEntity, Long>, JpaSpecificationExecutor<RoomJpaEntity> {

    List<RoomJpaEntity> findByStatusAndApprovedTrue(String status, Pageable pageable);

    @Query("SELECT r FROM RoomJpaEntity r WHERE r.status = 'AVAILABLE' AND r.approved = true AND r.provinceCode = :provinceCode AND r.id != :roomId ORDER BY r.ratingAvg DESC")
    List<RoomJpaEntity> findRecommendations(@Param("provinceCode") String provinceCode, @Param("roomId") Long roomId, Pageable pageable);

    Page<RoomJpaEntity> findByLandlordId(Long landlordId, Pageable pageable);

    List<RoomJpaEntity> findAllByIdIn(Collection<Long> ids);

    Optional<RoomJpaEntity> findByIdAndLandlordIdAndDeletedAtIsNull(Long id, Long landlordId);

    Optional<RoomJpaEntity> findBySlugAndDeletedAtIsNull(String slug);

    Optional<RoomJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
        SELECT r FROM RoomJpaEntity r
        WHERE r.provinceCode = :provinceCode
          AND r.id != :roomId
          AND r.deletedAt IS NULL
          AND r.hiddenAt IS NULL
          AND r.approved = true
          AND (r.status = 'AVAILABLE' OR r.status = 'PUBLISHED')
          AND r.price BETWEEN :minPrice AND :maxPrice
        ORDER BY r.ratingAvg DESC, r.id DESC
    """)
    List<RoomJpaEntity> findSimilarRooms(
            @Param("provinceCode") String provinceCode,
            @Param("roomId") Long roomId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    @Query("""
        SELECT p.code, p.name, p.fullName, p.slug, p.imageUrl, COUNT(r.id)
        FROM ProvinceJpaEntity p
        LEFT JOIN RoomJpaEntity r ON r.provinceCode = p.code AND r.deletedAt IS NULL AND r.hiddenAt IS NULL AND r.approved = true AND (r.status = 'AVAILABLE' OR r.status = 'PUBLISHED')
        GROUP BY p.code, p.name, p.fullName, p.slug, p.imageUrl, p.id
        ORDER BY COUNT(r.id) DESC, CASE WHEN p.slug IS NOT NULL THEN 0 ELSE 1 END, p.id ASC
    """)
    List<Object[]> findHotAreas();

    @Query("SELECT COUNT(r) FROM RoomJpaEntity r WHERE r.deletedAt IS NULL AND r.hiddenAt IS NULL AND r.approved = true AND (r.status = 'AVAILABLE' OR r.status = 'PUBLISHED')")
    long countActiveRooms();

    @Query("SELECT COUNT(DISTINCT r.provinceCode) FROM RoomJpaEntity r WHERE r.deletedAt IS NULL AND r.hiddenAt IS NULL AND r.approved = true AND (r.status = 'AVAILABLE' OR r.status = 'PUBLISHED')")
    long countActiveProvinces();

    @Query("SELECT COUNT(DISTINCT r.landlordId) FROM RoomJpaEntity r WHERE r.deletedAt IS NULL AND r.hiddenAt IS NULL AND r.approved = true")
    long countVerifiedLandlords();

    @Query("""
        SELECT r.roomTypeCode, COUNT(r.id)
        FROM RoomJpaEntity r
        WHERE r.deletedAt IS NULL AND r.hiddenAt IS NULL AND r.approved = true
          AND (r.status = 'AVAILABLE' OR r.status = 'PUBLISHED')
        GROUP BY r.roomTypeCode
    """)
    List<Object[]> countActiveByRoomTypeCode();

    @Query("""
        SELECT MIN(r.price), MAX(r.price)
        FROM RoomJpaEntity r
        WHERE r.deletedAt IS NULL AND r.hiddenAt IS NULL AND r.approved = true
          AND (r.status = 'AVAILABLE' OR r.status = 'PUBLISHED')
    """)
    List<Object[]> findActivePriceRange();

    @Query("""
        SELECT a.id, a.code, a.name, COUNT(r.id)
        FROM RoomJpaEntity r
        JOIN r.amenities a
        WHERE r.deletedAt IS NULL AND r.hiddenAt IS NULL AND r.approved = true
          AND (r.status = 'AVAILABLE' OR r.status = 'PUBLISHED')
        GROUP BY a.id, a.code, a.name
        ORDER BY COUNT(r.id) DESC
    """)
    List<Object[]> countActiveByAmenity();

    @Query("""
        SELECT p.code, p.name, COUNT(r.id)
        FROM RoomJpaEntity r
        JOIN ProvinceJpaEntity p ON p.code = r.provinceCode
        WHERE r.deletedAt IS NULL AND r.hiddenAt IS NULL AND r.approved = true
          AND (r.status = 'AVAILABLE' OR r.status = 'PUBLISHED')
        GROUP BY p.code, p.name
        ORDER BY COUNT(r.id) DESC
    """)
    List<Object[]> countActiveByProvince();

    @Query("""
        SELECT r FROM RoomJpaEntity r
        WHERE r.landlordId = :landlordId
          AND r.deletedAt IS NULL
          AND (:status IS NULL OR r.status = :status)
          AND (:search IS NULL OR LOWER(r.titleSearch) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<RoomJpaEntity> findLandlordRooms(
            @Param("landlordId") Long landlordId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );

    long countByLandlordIdAndDeletedAtIsNull(Long landlordId);

    long countByLandlordIdAndDeletedAtIsNullAndStatus(Long landlordId, String status);

    @Modifying
    @Query("UPDATE RoomJpaEntity r SET r.bookmarkCount = r.bookmarkCount + 1 WHERE r.id = :roomId")
    int incrementBookmarkCount(@Param("roomId") Long roomId);

    @Modifying
    @Query("UPDATE RoomJpaEntity r SET r.bookmarkCount = CASE WHEN r.bookmarkCount > 0 THEN r.bookmarkCount - 1 ELSE 0 END WHERE r.id = :roomId")
    int decrementBookmarkCount(@Param("roomId") Long roomId);
}

