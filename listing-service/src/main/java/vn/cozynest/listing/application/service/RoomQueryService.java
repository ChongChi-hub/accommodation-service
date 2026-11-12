package vn.cozynest.listing.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.listing.application.dto.*;
import vn.cozynest.listing.domain.model.DomainPage;
import vn.cozynest.listing.domain.model.Province;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.model.RoomSearchQuery;
import vn.cozynest.listing.domain.repository.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomQueryService {

    private final RoomRepository roomRepository;
    private final AmenityRepository amenityRepository;
    private final ReviewRepository reviewRepository;
    private final ProvinceRepository provinceRepository;
    private final LandlordSnapshotRepository landlordSnapshotRepository;
    private final RoomDtoMapper roomDtoMapper;

    public Page<RoomDto> searchRooms(RoomFilterParams params) {
        int page = params.getPage() != null && params.getPage() >= 0 ? params.getPage() : 0;
        int requestedSize = params.getSize() != null && params.getSize() > 0 ? params.getSize() : 12;
        int size = Math.min(requestedSize, 50);

        String sortKey = params.getEffectiveSort();
        RoomSearchQuery query = RoomSearchQuery.builder()
                .q(params.getQ())
                .provinceId(params.getProvinceId())
                .provinceCode(params.getProvinceCode())
                .provinceSlug(params.getProvinceSlug())
                .wardIds(params.getWardIds())
                .wardCode(params.getWardCode())
                .districtCode(params.getDistrictCode())
                .roomTypeIds(params.getRoomTypeIds())
                .roomTypeCode(params.getRoomTypeCode())
                .priceMin(params.getEffectivePriceMin())
                .priceMax(params.getEffectivePriceMax())
                .priceUnit(params.getPriceUnit())
                .areaMin(params.getEffectiveAreaMin())
                .areaMax(params.getEffectiveAreaMax())
                .bedrooms(params.getBedrooms())
                .occupants(params.getOccupants())
                .minAvailableSpots(params.getMinAvailableSpots())
                .amenityIds(params.getAmenityIds())
                .amenityMode(params.getAmenityMode())
                .allowPets(params.getAllowPets())
                .allowCooking(params.getAllowCooking())
                .genderPreference(params.getGenderPreference())
                .furnishing(params.getFurnishing())
                .includeUnavailable(params.getIncludeUnavailable())
                .sort(sortKey)
                .page(page)
                .size(size)
                .build();

        Map<String, String> provinceNames = buildProvinceNameMap();
        DomainPage<Room> roomPage = roomRepository.searchRooms(query, page, size, sortKey);
        List<Long> landlordIds = roomPage.content().stream()
                .map(Room::getLandlordId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Boolean> landlordVerifiedMap = (landlordIds.isEmpty() || landlordSnapshotRepository == null) ? Map.of() :
                landlordSnapshotRepository.findByLandlordIdIn(landlordIds).stream()
                        .collect(Collectors.toMap(
                                vn.cozynest.listing.domain.model.LandlordSnapshot::getLandlordId,
                                vn.cozynest.listing.domain.model.LandlordSnapshot::isVerified,
                                (a, b) -> a
                        ));

        List<RoomDto> dtos = roomPage.content().stream()
                .map(r -> roomDtoMapper.toDto(r, provinceNames, landlordVerifiedMap))
                .toList();
        return new PageImpl<>(dtos, PageRequest.of(page, size), roomPage.totalElements());
    }

    public RoomDto getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng với id: " + id));
        if (room.isDeleted() || room.isHidden() || (!room.isPublished() && !"AVAILABLE".equalsIgnoreCase(room.getStatus()))) {
            throw new NoSuchElementException("Phòng không tồn tại hoặc đã tạm dừng hiển thị: " + id);
        }
        boolean isVerified = false;
        if (room.getLandlordId() != null && landlordSnapshotRepository != null) {
            isVerified = landlordSnapshotRepository.findByLandlordId(room.getLandlordId())
                    .map(vn.cozynest.listing.domain.model.LandlordSnapshot::isVerified)
                    .orElse(false);
        }
        Map<Long, Boolean> map = (room.getLandlordId() != null) ? Map.of(room.getLandlordId(), isVerified) : Map.of();
        return roomDtoMapper.toDto(room, buildProvinceNameMap(), map);
    }

    public List<RoomDto> getRecommendations(Long roomId) {
        Room current = roomRepository.findById(roomId).orElse(null);
        String provinceCode = current != null ? current.getProvinceCode() : "HN";
        List<Room> recs = roomRepository.findRecommendations(provinceCode, roomId, 4);
        List<Long> landlordIds = recs.stream().map(Room::getLandlordId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Boolean> verifiedMap = (landlordIds.isEmpty() || landlordSnapshotRepository == null) ? Map.of() :
                landlordSnapshotRepository.findByLandlordIdIn(landlordIds).stream()
                        .collect(Collectors.toMap(vn.cozynest.listing.domain.model.LandlordSnapshot::getLandlordId, vn.cozynest.listing.domain.model.LandlordSnapshot::isVerified, (a, b) -> a));
        return recs.stream().map(r -> roomDtoMapper.toDto(r, Map.of(), verifiedMap)).collect(Collectors.toList());
    }

    public List<AmenityDto> getAllAmenities() {
        return amenityRepository.findAll().stream().map(a -> AmenityDto.builder()
                .id(a.getId())
                .code(a.getCode())
                .name(a.getName())
                .icon(a.getIcon())
                .category(a.getCategory())
                .build()).collect(Collectors.toList());
    }

    public List<ReviewDto> getReviewsByRoom(Long roomId) {
        return reviewRepository.findByRoomIdOrderByCreatedAtDesc(roomId).stream()
                .map(r -> ReviewDto.builder()
                        .id(r.getId())
                        .roomId(r.getRoomId())
                        .tenantId(r.getTenantId())
                        .tenantName(r.getTenantName())
                        .tenantAvatar(r.getTenantAvatar())
                        .rating(r.getRating())
                        .comment(r.getComment())
                        .landlordReply(r.getLandlordReply())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public RoomFacetsDto getFacets() {
        List<Object[]> provRows = roomRepository.countActiveByProvince();
        List<RoomFacetsDto.FacetItemDto> provinces = provRows.stream().map(row -> RoomFacetsDto.FacetItemDto.builder()
                .code((String) row[0])
                .name((String) row[1])
                .count(((Number) row[2]).longValue())
                .build()).collect(Collectors.toList());

        List<Object[]> priceRows = roomRepository.findActivePriceRange();
        BigDecimal min = BigDecimal.valueOf(1000000);
        BigDecimal max = BigDecimal.valueOf(25000000);
        if (!priceRows.isEmpty() && priceRows.get(0) != null) {
            Object[] r = priceRows.get(0);
            if (r[0] != null) min = (BigDecimal) r[0];
            if (r[1] != null) max = (BigDecimal) r[1];
        }
        RoomFacetsDto.PriceRangeFacetDto priceRange = RoomFacetsDto.PriceRangeFacetDto.builder()
                .min(min)
                .max(max)
                .build();

        List<Object[]> amenRows = roomRepository.countActiveByAmenity();
        List<RoomFacetsDto.AmenityFacetDto> amenities = amenRows.stream().map(row -> RoomFacetsDto.AmenityFacetDto.builder()
                .id(((Number) row[0]).longValue())
                .code((String) row[1])
                .name((String) row[2])
                .count(((Number) row[3]).longValue())
                .build()).collect(Collectors.toList());

        List<Object[]> typeRows = roomRepository.countActiveByRoomTypeCode();
        Map<String, Long> typeCountMap = new HashMap<>();
        for (Object[] row : typeRows) {
            String code = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            typeCountMap.put(code, count);
        }
        List<RoomFacetsDto.FacetItemDto> roomTypes = typeCountMap.entrySet().stream()
                .map(e -> RoomFacetsDto.FacetItemDto.builder()
                        .code(e.getKey())
                        .name(e.getKey())
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());

        long phongTroCount = typeCountMap.getOrDefault("SINGLE", 0L);
        long nhaNguyenCanCount = typeCountMap.getOrDefault("FULL_HOUSE", 0L);
        long canHoCount = typeCountMap.getOrDefault("MINI_APARTMENT", 0L) + typeCountMap.getOrDefault("STUDIO", 0L) + typeCountMap.getOrDefault("APARTMENT", 0L);
        long oGhepCount = typeCountMap.getOrDefault("SHARED", 0L);

        List<RoomFacetsDto.FacetItemDto> categories = List.of(
                RoomFacetsDto.FacetItemDto.builder().code("phong-tro").name("Phòng trọ").count(phongTroCount).build(),
                RoomFacetsDto.FacetItemDto.builder().code("nha-nguyen-can").name("Nhà nguyên căn").count(nhaNguyenCanCount).build(),
                RoomFacetsDto.FacetItemDto.builder().code("can-ho").name("Căn hộ").count(canHoCount).build(),
                RoomFacetsDto.FacetItemDto.builder().code("o-ghep").name("Ở ghép").count(oGhepCount).build()
        );

        return RoomFacetsDto.builder()
                .provinces(provinces)
                .priceRange(priceRange)
                .amenities(amenities)
                .roomTypes(roomTypes)
                .categories(categories)
                .build();
    }

    public List<RoomDto> toRoomDtos(List<Room> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return List.of();
        }
        Map<String, String> provinceNames = buildProvinceNameMap();
        List<Long> landlordIds = rooms.stream()
                .map(Room::getLandlordId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Boolean> landlordVerifiedMap = (landlordIds.isEmpty() || landlordSnapshotRepository == null)
                ? Map.of()
                : landlordSnapshotRepository.findByLandlordIdIn(landlordIds).stream()
                        .collect(Collectors.toMap(
                                vn.cozynest.listing.domain.model.LandlordSnapshot::getLandlordId,
                                vn.cozynest.listing.domain.model.LandlordSnapshot::isVerified,
                                (a, b) -> a
                        ));
        return rooms.stream()
                .map(r -> roomDtoMapper.toDto(r, provinceNames, landlordVerifiedMap))
                .toList();
    }

    private Map<String, String> buildProvinceNameMap() {
        if (provinceRepository == null) return Map.of();
        return provinceRepository.findAll().stream()
                .filter(p -> p.getCode() != null && p.getName() != null)
                .collect(Collectors.toMap(Province::getCode, Province::getName, (a, b) -> a));
    }
}
