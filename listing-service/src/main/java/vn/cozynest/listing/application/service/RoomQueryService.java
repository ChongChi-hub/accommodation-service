package vn.cozynest.listing.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.listing.application.dto.*;
import vn.cozynest.listing.domain.model.Province;
import vn.cozynest.listing.domain.model.Room;
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
    private final RoomDtoMapper roomDtoMapper;
    private final RoomSpecificationBuilder roomSpecificationBuilder;

    public Page<RoomDto> searchRooms(RoomFilterParams params) {
        int page = params.getPage() != null && params.getPage() >= 0 ? params.getPage() : 0;
        int requestedSize = params.getSize() != null && params.getSize() > 0 ? params.getSize() : 12;
        int size = Math.min(requestedSize, 50);

        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        String sortKey = params.getEffectiveSort();
        if ("price_asc".equalsIgnoreCase(sortKey) || "priceAsc".equalsIgnoreCase(sortKey)) {
            sort = Sort.by(Sort.Order.asc("price"), Sort.Order.desc("id"));
        } else if ("price_desc".equalsIgnoreCase(sortKey) || "priceDesc".equalsIgnoreCase(sortKey)) {
            sort = Sort.by(Sort.Order.desc("price"), Sort.Order.desc("id"));
        } else if ("areaAsc".equalsIgnoreCase(sortKey)) {
            sort = Sort.by(Sort.Order.asc("area"), Sort.Order.desc("id"));
        } else if ("areaDesc".equalsIgnoreCase(sortKey)) {
            sort = Sort.by(Sort.Order.desc("area"), Sort.Order.desc("id"));
        } else if ("rating".equalsIgnoreCase(sortKey)) {
            sort = Sort.by(Sort.Order.desc("ratingAvg"), Sort.Order.desc("id"));
        }

        Specification<Room> spec = roomSpecificationBuilder.buildSpecification(params, provinceRepository);
        Map<String, String> provinceNames = buildProvinceNameMap();
        Pageable pageable = PageRequest.of(page, size, sort);

        return roomRepository.findAll(spec, pageable).map(r -> roomDtoMapper.toDto(r, provinceNames));
    }

    public RoomDto getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng với id: " + id));
        if (room.isDeleted() || room.isHidden() || (!room.isPublished() && !"AVAILABLE".equalsIgnoreCase(room.getStatus()))) {
            throw new NoSuchElementException("Phòng không tồn tại hoặc đã tạm dừng hiển thị: " + id);
        }
        return roomDtoMapper.toDto(room);
    }

    public List<RoomDto> getRecommendations(Long roomId) {
        Room current = roomRepository.findById(roomId).orElse(null);
        String provinceCode = current != null ? current.getProvinceCode() : "HN";
        return roomRepository.findRecommendations(provinceCode, roomId, PageRequest.of(0, 4))
                .stream().map(roomDtoMapper::toDto).collect(Collectors.toList());
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

        long phongTroCount = typeCountMap.getOrDefault("SINGLE", 0L) + typeCountMap.getOrDefault("HOMESTAY", 0L);
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

    private Map<String, String> buildProvinceNameMap() {
        if (provinceRepository == null) return Map.of();
        return provinceRepository.findAll().stream()
                .filter(p -> p.getCode() != null && p.getName() != null)
                .collect(Collectors.toMap(Province::getCode, Province::getName, (a, b) -> a));
    }
}
