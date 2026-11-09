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
        return toDto(r, Map.of());
    }

    public RoomDto toDto(Room r, Map<String, String> provinceNames) {
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
                .ratingAvg(r.getRatingAvg())
                .reviewCount(r.getReviewCount())
                .bookmarkCount(r.getBookmarkCount() != null ? r.getBookmarkCount() : 0)
                .primaryImageUrl(primaryImg)
                .imageUrls(images)
                .amenities(ams)
                .build();
    }
}

