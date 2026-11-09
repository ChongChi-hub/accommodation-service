package vn.cozynest.listing.application.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.common.dto.PaginationResponse;
import vn.cozynest.listing.application.dto.*;
import vn.cozynest.listing.domain.model.*;
import vn.cozynest.listing.domain.repository.*;

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

        String resolvedProvinceCode = resolveProvinceCode(params.getProvinceCode(), params.getProvinceSlug());
        List<String> resolvedRoomTypes = expandRoomTypeCodes(params.getRoomTypeCode());
        String sanitizedQuery = sanitizeQuery(params.getQ());
        Map<String, String> provinceNames = buildProvinceNameMap();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Room> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("approved"), true));
            predicates.add(cb.or(cb.equal(root.get("status"), "AVAILABLE"), cb.equal(root.get("status"), "PUBLISHED")));
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.isNull(root.get("hiddenAt")));

            if (sanitizedQuery != null) {
                String pattern = "%" + sanitizedQuery.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("titleSearch")), pattern, '\\'),
                        cb.like(cb.lower(root.get("address")), pattern, '\\'),
                        cb.like(cb.lower(root.get("description").as(String.class)), pattern, '\\')
                ));
            }
            if (resolvedProvinceCode != null) {
                predicates.add(cb.equal(root.get("provinceCode"), resolvedProvinceCode));
            }
            if (params.getDistrictCode() != null && !params.getDistrictCode().isBlank()) {
                predicates.add(cb.equal(root.get("districtCode"), params.getDistrictCode().trim()));
            }
            if (params.getWardCode() != null && !params.getWardCode().isBlank()) {
                predicates.add(cb.equal(root.get("wardCode"), params.getWardCode().trim()));
            }
            if (!resolvedRoomTypes.isEmpty()) {
                predicates.add(root.get("roomTypeCode").in(resolvedRoomTypes));
            }
            if (params.getEffectivePriceMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), params.getEffectivePriceMin()));
            }
            if (params.getEffectivePriceMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), params.getEffectivePriceMax()));
            }
            if (params.getEffectiveAreaMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), params.getEffectiveAreaMin()));
            }
            if (params.getEffectiveAreaMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("area"), params.getEffectiveAreaMax()));
            }
            if (params.getOccupants() != null && params.getOccupants() > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxOccupants"), params.getOccupants()));
            }
            if (params.getAllowPets() != null) {
                predicates.add(cb.equal(root.get("allowPets"), params.getAllowPets()));
            }
            if (params.getAllowCooking() != null) {
                predicates.add(cb.equal(root.get("allowCooking"), params.getAllowCooking()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return roomRepository.findAll(spec, pageable).map(r -> toDto(r, provinceNames));
    }

    private String resolveProvinceCode(String provinceCode, String provinceSlug) {
        if (provinceCode != null && !provinceCode.isBlank()) {
            return provinceCode.trim().toUpperCase(Locale.ROOT);
        }
        if (provinceSlug == null || provinceSlug.isBlank()) {
            return null;
        }
        String candidate = provinceSlug.trim();
        if (provinceRepository != null) {
            return provinceRepository.findBySlugIgnoreCase(candidate)
                    .or(() -> provinceRepository.findByCodeIgnoreCase(candidate))
                    .map(Province::getCode)
                    .orElse(candidate.toUpperCase(Locale.ROOT));
        }
        return candidate.toUpperCase(Locale.ROOT);
    }

    private List<String> expandRoomTypeCodes(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        Set<String> codes = new LinkedHashSet<>();
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;
            switch (trimmed.toLowerCase(Locale.ROOT)) {
                case "phong-tro" -> {
                    codes.add("SINGLE");
                    codes.add("HOMESTAY");
                }
                case "nha-nguyen-can" -> codes.add("FULL_HOUSE");
                case "can-ho" -> {
                    codes.add("MINI_APARTMENT");
                    codes.add("STUDIO");
                    codes.add("APARTMENT");
                }
                case "o-ghep" -> codes.add("SHARED");
                default -> codes.add(trimmed.toUpperCase(Locale.ROOT));
            }
        }
        return List.copyOf(codes);
    }

    private String sanitizeQuery(String q) {
        if (q == null) return null;
        String trimmed = q.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > 100) {
            trimmed = trimmed.substring(0, 100);
        }
        return trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Map<String, String> buildProvinceNameMap() {
        if (provinceRepository == null) return Map.of();
        return provinceRepository.findAll().stream()
                .filter(p -> p.getCode() != null && p.getName() != null)
                .collect(Collectors.toMap(Province::getCode, Province::getName, (a, b) -> a));
    }

    public RoomDto getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng với id: " + id));
        if (room.isDeleted() || room.isHidden() || (!room.isPublished() && !"AVAILABLE".equalsIgnoreCase(room.getStatus()))) {
            throw new NoSuchElementException("Phòng không tồn tại hoặc đã tạm dừng hiển thị: " + id);
        }
        return toDto(room);
    }

    public List<RoomDto> getRecommendations(Long roomId) {
        Room current = roomRepository.findById(roomId).orElse(null);
        String provinceCode = current != null ? current.getProvinceCode() : "HN";
        return roomRepository.findRecommendations(provinceCode, roomId, PageRequest.of(0, 4))
                .stream().map(this::toDto).collect(Collectors.toList());
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

    private RoomDto toDto(Room r) {
        return toDto(r, Map.of());
    }

    private RoomDto toDto(Room r, Map<String, String> provinceNames) {
        List<String> images = r.getImages().stream()
                .sorted(Comparator.comparingInt(img -> img.getDisplayOrder() != null ? img.getDisplayOrder() : 0))
                .map(RoomImage::getImageUrl)
                .collect(Collectors.toList());

        String primaryImg = r.getImages().stream()
                .filter(RoomImage::isPrimary)
                .findFirst()
                .map(RoomImage::getImageUrl)
                .orElse(!images.isEmpty() ? images.get(0) : "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80");

        List<AmenityDto> ams = r.getAmenities().stream().map(a -> AmenityDto.builder()
                .id(a.getId())
                .code(a.getCode())
                .name(a.getName())
                .icon(a.getIcon())
                .category(a.getCategory())
                .build()).collect(Collectors.toList());

        return RoomDto.builder()
                .id(r.getId())
                .slug(r.getSlug())
                .landlordId(r.getLandlordId())
                .landlordName(r.getLandlordName())
                .landlordPhone(r.getLandlordPhone())
                .roomTypeCode(r.getRoomTypeCode())
                .title(r.getTitle())
                .description(r.getDescription())
                .address(r.getAddress())
                .provinceCode(r.getProvinceCode())
                .provinceName(r.getProvinceCode() != null ? provinceNames.getOrDefault(r.getProvinceCode(), r.getProvinceCode()) : null)
                .districtCode(r.getDistrictCode())
                .wardCode(r.getWardCode())
                .wardId(r.getWardId())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .area(r.getArea())
                .maxOccupants(r.getMaxOccupants())
                .price(r.getPrice())
                .deposit(r.getDeposit())
                .electricityCost(r.getElectricityCost())
                .waterCost(r.getWaterCost())
                .internetCost(r.getInternetCost())
                .serviceCost(r.getServiceCost())
                .status(r.getStatus())
                .approved(r.isApproved())
                .allowPets(r.isAllowPets())
                .allowCooking(r.isAllowCooking())
                .ratingAvg(r.getRatingAvg())
                .reviewCount(r.getReviewCount())
                .bookmarkCount(r.getBookmarkCount() != null ? r.getBookmarkCount() : 0)
                .primaryImageUrl(primaryImg)
                .imageUrls(images)
                .amenities(ams)
                .build();
    }
}

