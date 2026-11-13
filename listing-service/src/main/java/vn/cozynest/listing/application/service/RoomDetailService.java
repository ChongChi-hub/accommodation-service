package vn.cozynest.listing.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.listing.application.dto.*;
import vn.cozynest.listing.domain.model.*;
import vn.cozynest.listing.domain.repository.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomDetailService {

    private final RoomRepository roomRepository;
    private final RoomPriceRepository roomPriceRepository;
    private final LandlordSnapshotRepository landlordSnapshotRepository;
    private final ReviewRepository reviewRepository;
    private final ProvinceRepository provinceRepository;
    private final RoomTypeRepository roomTypeRepository;

    public RoomDetailDto getRoomDetail(String slugOrId) {
        Room room;
        if (slugOrId.matches("\\d+")) {
            Long id = Long.parseLong(slugOrId);
            room = roomRepository.findByIdAndDeletedAtIsNull(id)
                    .orElseGet(() -> roomRepository.findBySlugAndDeletedAtIsNull(slugOrId)
                            .orElseGet(() -> roomRepository.findByRoomUuidAndDeletedAtIsNull(slugOrId).orElse(null)));
        } else {
            room = roomRepository.findBySlugAndDeletedAtIsNull(slugOrId)
                    .orElseGet(() -> roomRepository.findByRoomUuidAndDeletedAtIsNull(slugOrId).orElse(null));
        }

        if (room == null || room.isDeleted() || room.isHidden() || !room.isApproved()) {
            throw new NoSuchElementException("Không tìm thấy phòng trọ hoặc tin đăng đã bị ẩn: " + slugOrId);
        }

        boolean isPublicStatus = "PUBLISHED".equalsIgnoreCase(room.getPublicationStatus())
                || "PUBLISHED".equalsIgnoreCase(room.getStatus())
                || "AVAILABLE".equalsIgnoreCase(room.getStatus())
                || "OCCUPIED".equalsIgnoreCase(room.getStatus())
                || "MAINTENANCE".equalsIgnoreCase(room.getStatus());

        if (!isPublicStatus) {
            throw new NoSuchElementException("Phòng trọ đang ở trạng thái nháp hoặc chờ duyệt: " + slugOrId);
        }

        List<RoomFeeDto> fees = getRoomFees(room);
        LandlordSnapshotDto landlord = getLandlordSnapshot(room);
        List<ReviewDto> reviews = getRoomReviews(room.getId());
        List<RoomImageDto> images = getRoomImages(room);

        List<AmenityDto> amenities = room.getAmenities().stream()
                .sorted(Comparator.comparing(Amenity::getCategory).thenComparing(Amenity::getName))
                .map(a -> AmenityDto.builder().id(a.getId()).code(a.getCode()).name(a.getName()).icon(a.getIcon()).category(a.getCategory()).build())
                .collect(Collectors.toList());

        String resolvedProvinceName = provinceRepository.findByCodeIgnoreCase(room.getProvinceCode())
                .map(Province::getName)
                .orElse(room.getProvinceCode());
        String resolvedRoomTypeName = roomTypeRepository.findByCodeIgnoreCase(room.getRoomTypeCode())
                .map(RoomType::getName)
                .orElse(room.getRoomTypeCode());

        return RoomDetailDto.builder()
                .id(room.getId())
                .roomUuid(room.getRoomUuid())
                .slug(room.getSlug())
                .title(room.getTitle())
                .description(room.getDescription())
                .address(room.getAddress())
                .provinceCode(room.getProvinceCode())
                .provinceName(resolvedProvinceName)
                .wardCode(room.getWardCode())
                .wardId(room.getWardId())
                .latitude(room.getLatitude())
                .longitude(room.getLongitude())
                .roomTypeId(room.getRoomTypeId())
                .roomTypeCode(room.getRoomTypeCode())
                .roomTypeName(resolvedRoomTypeName)
                .area(room.getArea())
                .floor(room.getFloor() != null ? room.getFloor() : 2)
                .bedrooms(room.getBedrooms() != null ? room.getBedrooms() : 1)
                .bathrooms(room.getBathrooms() != null ? room.getBathrooms() : 1)
                .maxOccupants(room.getMaxOccupants())
                .price(room.getPrice())
                .priceUnit(room.getPriceUnit() != null ? room.getPriceUnit() : "ROOM")
                .deposit(room.getDeposit())
                .electricityCost(room.getElectricityCost())
                .waterCost(room.getWaterCost())
                .internetCost(room.getInternetCost())
                .serviceCost(room.getServiceCost())
                .status(room.getStatus())
                .publicationStatus(room.getPublicationStatus())
                .occupancyStatus(room.getOccupancyStatus())
                .approved(room.isApproved())
                .allowPets(room.isAllowPets())
                .allowCooking(room.isAllowCooking())
                .genderPreference(room.getGenderPreference())
                .furnishing(room.getFurnishing())
                .curfew(room.getCurfew())
                .availableDate(room.getAvailableDate() != null ? room.getAvailableDate() : java.time.LocalDate.now())
                .minLeaseMonths(room.getMinLeaseMonths() != null ? room.getMinLeaseMonths() : 6)
                .ratingAvg(room.getRatingAvg())
                .reviewCount(room.getReviewCount())
                .ratingCleanliness(room.getRatingCleanliness() != null ? room.getRatingCleanliness() : 4.9)
                .ratingLocation(room.getRatingLocation() != null ? room.getRatingLocation() : 4.8)
                .ratingPriceQuality(room.getRatingPriceQuality() != null ? room.getRatingPriceQuality() : 4.7)
                .ratingSecurity(room.getRatingSecurity() != null ? room.getRatingSecurity() : 5.0)
                .viewCount(room.getViewCount())
                .bookmarkCount(room.getBookmarkCount())
                .publishedAt(room.getPublishedAt())
                .images(images)
                .amenities(amenities)
                .roomFees(fees)
                .landlord(landlord)
                .reviews(reviews)
                .build();
    }

    public List<RoomDto> getSimilarRooms(Long roomId) {
        Room current = roomRepository.findById(roomId).orElse(null);
        if (current == null) return List.of();

        BigDecimal price = current.getPrice();
        BigDecimal minPrice = price.multiply(BigDecimal.valueOf(0.7));
        BigDecimal maxPrice = price.multiply(BigDecimal.valueOf(1.3));

        List<Room> similar = roomRepository.findSimilarRooms(
                current.getProvinceCode(), roomId, minPrice, maxPrice, 4
        );

        if (similar.isEmpty()) {
            similar = roomRepository.findRecommendations(current.getProvinceCode(), roomId, 4);
        }

        return similar.stream().map(this::toSummaryDto).collect(Collectors.toList());
    }

    private List<RoomFeeDto> getRoomFees(Room room) {
        List<RoomPrice> dbPrices = roomPriceRepository.findByRoomId(room.getId());
        if (!dbPrices.isEmpty()) {
            return dbPrices.stream().map(p -> RoomFeeDto.builder()
                    .id(p.getId())
                    .roomId(p.getRoomId())
                    .label(p.getLabel())
                    .price(p.getPrice())
                    .unit(p.getUnit())
                    .billingMode(p.getBillingMode())
                    .metered(p.isMetered())
                    .meterType(p.getMeterType())
                    .build()).collect(Collectors.toList());
        }

        List<RoomFeeDto> defaults = new ArrayList<>();
        if (room.getElectricityCost() != null) {
            defaults.add(RoomFeeDto.builder().roomId(room.getId()).label("Tiền điện sinh hoạt").price(room.getElectricityCost()).unit("kWh").billingMode("METERED").metered(true).meterType("electric").build());
        }
        if (room.getWaterCost() != null) {
            defaults.add(RoomFeeDto.builder().roomId(room.getId()).label("Tiền nước sinh hoạt").price(room.getWaterCost()).unit("m³").billingMode("METERED").metered(true).meterType("water").build());
        }
        if (room.getInternetCost() != null) {
            defaults.add(RoomFeeDto.builder().roomId(room.getId()).label("Internet WiFi tốc độ cao").price(room.getInternetCost()).unit("tháng").billingMode("FIXED").metered(false).build());
        }
        if (room.getServiceCost() != null) {
            defaults.add(RoomFeeDto.builder().roomId(room.getId()).label("Phí dịch vụ chung & rác").price(room.getServiceCost()).unit("người/tháng").billingMode("PER_PERSON").metered(false).build());
        }
        return defaults;
    }

    private LandlordSnapshotDto getLandlordSnapshot(Room room) {
        return landlordSnapshotRepository.findByLandlordId(room.getLandlordId())
                .map(ls -> LandlordSnapshotDto.builder()
                        .id(ls.getId())
                        .landlordId(ls.getLandlordId())
                        .name(ls.getName())
                        .phone(ls.getPhone())
                        .avatarUrl(ls.getAvatarUrl())
                        .email(ls.getEmail())
                        .verified(ls.isVerified())
                        .ratingAvg(ls.getRatingAvg())
                        .reviewCount(ls.getReviewCount())
                        .responseRatePercent(ls.getResponseRatePercent())
                        .responseTimeMinutes(ls.getResponseTimeMinutes())
                        .roomCount(ls.getRoomCount())
                        .joinedAt(ls.getJoinedAt())
                        .build())
                .orElseGet(() -> LandlordSnapshotDto.builder()
                        .landlordId(room.getLandlordId())
                        .name(room.getLandlordName())
                        .phone(room.getLandlordPhone())
                        .avatarUrl("/uploads/avatars/default.png")
                        .verified(true)
                        .ratingAvg(4.9)
                        .reviewCount(12)
                        .responseRatePercent(98)
                        .responseTimeMinutes(15)
                        .roomCount(4)
                        .build());
    }

    private List<ReviewDto> getRoomReviews(Long roomId) {
        return reviewRepository.findByRoomIdOrderByCreatedAtDesc(roomId).stream()
                .map(r -> ReviewDto.builder()
                        .id(r.getId()).roomId(r.getRoomId()).tenantId(r.getTenantId())
                        .tenantName(r.getTenantName())
                        .tenantAvatar(r.getTenantAvatar() != null ? r.getTenantAvatar() : "/uploads/avatars/default.png")
                        .rating(r.getRating()).comment(r.getComment()).landlordReply(r.getLandlordReply())
                        .createdAt(r.getCreatedAt()).build())
                .collect(Collectors.toList());
    }

    private List<RoomImageDto> getRoomImages(Room room) {
        return room.getImages().stream()
                .sorted(Comparator.comparing(RoomImage::isCover).reversed()
                        .thenComparingInt(img -> img.getSortOrder() != null ? img.getSortOrder() : 0))
                .map(img -> RoomImageDto.builder()
                        .id(img.getId())
                        .roomId(img.getRoomId())
                        .imageUrl(img.getImageUrl())
                        .width(img.getWidth() != null ? img.getWidth() : 1280)
                        .height(img.getHeight() != null ? img.getHeight() : 720)
                        .cover(img.isCover())
                        .sortOrder(img.getSortOrder())
                        .altText(img.getAltText() != null ? img.getAltText() : room.getTitle())
                        .build())
                .collect(Collectors.toList());
    }

    private RoomDto toSummaryDto(Room r) {
        String primary = r.getImages().stream()
                .filter(RoomImage::isCover)
                .findFirst()
                .map(RoomImage::getImageUrl)
                .orElse(!r.getImages().isEmpty() ? r.getImages().iterator().next().getImageUrl() : "");

        return RoomDto.builder()
                .id(r.getId())
                .slug(r.getSlug())
                .title(r.getTitle())
                .price(r.getPrice())
                .area(r.getArea())
                .address(r.getAddress())
                .provinceCode(r.getProvinceCode())
                .status(r.getStatus())
                .ratingAvg(r.getRatingAvg())
                .reviewCount(r.getReviewCount())
                .primaryImageUrl(primary)
                .build();
    }
}
