package vn.cozynest.listing.application.service;

import org.springframework.stereotype.Component;
import vn.cozynest.listing.application.dto.AmenityDto;
import vn.cozynest.listing.application.dto.RoomDto;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.model.RoomImage;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class RoomDtoMapper {

    public RoomDto toDto(Room r) {
        return toDto(r, Map.of(), Map.of());
    }

    public RoomDto toDto(Room r, Map<String, String> provinceNames) {
        return toDto(r, provinceNames, Map.of());
    }

    public RoomDto toDto(Room r, Map<String, String> provinceNames, Map<Long, Boolean> landlordVerifiedMap) {
        List<String> images = r.getImages().stream()
                .sorted(Comparator.comparingInt(img -> img.getDisplayOrder() != null ? img.getDisplayOrder() : 0))
                .map(RoomImage::getImageUrl)
                .collect(Collectors.toList());

        String primaryImg = r.getImages().stream()
                .filter(RoomImage::isPrimary)
                .findFirst()
                .map(RoomImage::getImageUrl)
                .orElse(!images.isEmpty() ? images.get(0) : "/images/rooms/room-fallback-640.webp");

        List<AmenityDto> ams = r.getAmenities().stream().map(a -> AmenityDto.builder()
                .id(a.getId())
                .code(a.getCode())
                .name(a.getName())
                .icon(a.getIcon())
                .category(a.getCategory())
                .build()).collect(Collectors.toList());

        boolean isVerified = false;
        if (landlordVerifiedMap != null && r.getLandlordId() != null) {
            isVerified = Boolean.TRUE.equals(landlordVerifiedMap.get(r.getLandlordId()));
        }

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
                .floor(r.getFloor())
                .bedrooms(r.getBedrooms())
                .maxOccupants(r.getMaxOccupants())
                .availableSpots(r.getAvailableSpots())
                .totalSpots(r.getTotalSpots())
                .price(r.getPrice())
                .priceUnit(r.getPriceUnit() != null ? r.getPriceUnit() : "ROOM")
                .deposit(r.getDeposit())
                .electricityCost(r.getElectricityCost())
                .waterCost(r.getWaterCost())
                .internetCost(r.getInternetCost())
                .serviceCost(r.getServiceCost())
                .status(r.getStatus())
                .approved(r.isApproved())
                .allowPets(r.isAllowPets())
                .allowCooking(r.isAllowCooking())
                .genderPreference(r.getGenderPreference())
                .furnishing(r.getFurnishing())
                .ratingAvg(r.getRatingAvg())
                .reviewCount(r.getReviewCount())
                .bookmarkCount(r.getBookmarkCount() != null ? r.getBookmarkCount() : 0)
                .primaryImageUrl(primaryImg)
                .imageUrls(images)
                .amenities(ams)
                .isLandlordVerified(isVerified)
                .createdAt(r.getCreatedAt())
                .publishedAt(r.getPublishedAt())
                .build();
    }
}
