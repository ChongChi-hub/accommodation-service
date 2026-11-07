package vn.toam.listing.application.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.toam.common.dto.PaginationResponse;
import vn.toam.listing.application.dto.*;
import vn.toam.listing.domain.model.*;
import vn.toam.listing.domain.repository.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomQueryService {

    private final RoomRepository roomRepository;
    private final AmenityRepository amenityRepository;
    private final ReviewRepository reviewRepository;

    public Page<RoomDto> searchRooms(RoomFilterParams params) {
        int page = params.getPage() != null && params.getPage() >= 0 ? params.getPage() : 0;
        int size = params.getSize() != null && params.getSize() > 0 ? params.getSize() : 12;

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("price_asc".equalsIgnoreCase(params.getSortBy())) {
            sort = Sort.by(Sort.Direction.ASC, "price");
        } else if ("price_desc".equalsIgnoreCase(params.getSortBy())) {
            sort = Sort.by(Sort.Direction.DESC, "price");
        } else if ("rating".equalsIgnoreCase(params.getSortBy())) {
            sort = Sort.by(Sort.Direction.DESC, "ratingAvg");
        }

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Room> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("approved"), true));
            predicates.add(cb.equal(root.get("status"), "AVAILABLE"));

            if (params.getProvinceCode() != null && !params.getProvinceCode().isBlank()) {
                predicates.add(cb.equal(root.get("provinceCode"), params.getProvinceCode()));
            }
            if (params.getDistrictCode() != null && !params.getDistrictCode().isBlank()) {
                predicates.add(cb.equal(root.get("districtCode"), params.getDistrictCode()));
            }
            if (params.getRoomTypeCode() != null && !params.getRoomTypeCode().isBlank()) {
                predicates.add(cb.equal(root.get("roomTypeCode"), params.getRoomTypeCode()));
            }
            if (params.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), params.getMinPrice()));
            }
            if (params.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), params.getMaxPrice()));
            }
            if (params.getMinArea() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), params.getMinArea()));
            }
            if (params.getMaxArea() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("area"), params.getMaxArea()));
            }
            if (params.getAllowPets() != null) {
                predicates.add(cb.equal(root.get("allowPets"), params.getAllowPets()));
            }
            if (params.getAllowCooking() != null) {
                predicates.add(cb.equal(root.get("allowCooking"), params.getAllowCooking()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return roomRepository.findAll(spec, pageable).map(this::toDto);
    }

    public RoomDto getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phòng với id: " + id));
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
                .landlordId(r.getLandlordId())
                .landlordName(r.getLandlordName())
                .landlordPhone(r.getLandlordPhone())
                .roomTypeCode(r.getRoomTypeCode())
                .title(r.getTitle())
                .description(r.getDescription())
                .address(r.getAddress())
                .provinceCode(r.getProvinceCode())
                .districtCode(r.getDistrictCode())
                .wardCode(r.getWardCode())
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
                .primaryImageUrl(primaryImg)
                .imageUrls(images)
                .amenities(ams)
                .build();
    }
}

