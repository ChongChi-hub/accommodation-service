package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.DomainPage;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.model.RoomSearchQuery;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {

    DomainPage<Room> searchRooms(RoomSearchQuery query, int page, int size, String sortKey);

    List<Room> findByStatusAndApprovedTrue(String status, int page, int size);

    List<Room> findRecommendations(String provinceCode, Long roomId, int limit);

    DomainPage<Room> findByLandlordId(Long landlordId, int page, int size);

    Optional<Room> findById(Long id);

    default List<Room> findAll() {
        return java.util.Collections.emptyList();
    }

    List<Room> findAllByIdIn(Collection<Long> ids);

    Optional<Room> findByIdAndLandlordIdAndDeletedAtIsNull(Long id, Long landlordId);

    Optional<Room> findBySlugAndDeletedAtIsNull(String slug);

    default Optional<Room> findByRoomUuidAndDeletedAtIsNull(String roomUuid) {
        return Optional.empty();
    }

    Optional<Room> findByIdAndDeletedAtIsNull(Long id);

    Room save(Room room);

    List<Room> findSimilarRooms(String provinceCode, Long roomId, BigDecimal minPrice, BigDecimal maxPrice, int limit);

    List<Object[]> findHotAreas();

    long countActiveRooms();

    long countActiveProvinces();

    long countVerifiedLandlords();

    List<Object[]> countActiveByRoomTypeCode();

    List<Object[]> findActivePriceRange();

    List<Object[]> countActiveByAmenity();

    List<Object[]> countActiveByProvince();

    DomainPage<Room> findLandlordRooms(
            Long landlordId,
            String status,
            String search,
            int page,
            int size,
            String sortField,
            boolean ascending
    );

    long countByLandlordIdAndDeletedAtIsNull(Long landlordId);

    long countByLandlordIdAndDeletedAtIsNullAndStatus(Long landlordId, String status);

    int incrementBookmarkCount(Long roomId);

    int decrementBookmarkCount(Long roomId);
}
