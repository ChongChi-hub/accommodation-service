package vn.cozynest.listing.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import vn.cozynest.listing.domain.model.*;
import vn.cozynest.listing.infrastructure.persistence.entity.*;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RoomPersistenceMapper {

    public Amenity toDomain(AmenityJpaEntity e) {
        if (e == null) return null;
        return Amenity.builder()
                .id(e.getId()).code(e.getCode()).name(e.getName()).icon(e.getIcon()).category(e.getCategory())
                .build();
    }

    public AmenityJpaEntity toEntity(Amenity d) {
        if (d == null) return null;
        return AmenityJpaEntity.builder()
                .id(d.getId()).code(d.getCode()).name(d.getName()).icon(d.getIcon()).category(d.getCategory())
                .build();
    }

    public RoomImage toDomain(RoomImageJpaEntity e) {
        if (e == null) return null;
        return RoomImage.builder()
                .id(e.getId()).roomId(e.getRoomId()).imageUrl(e.getImageUrl()).storageKey(e.getStorageKey())
                .thumbKey(e.getThumbKey()).mediumKey(e.getMediumKey()).largeKey(e.getLargeKey())
                .originalFilename(e.getOriginalFilename()).contentType(e.getContentType()).sizeBytes(e.getSizeBytes())
                .width(e.getWidth()).height(e.getHeight()).cover(e.isCover()).primary(e.isPrimary())
                .sortOrder(e.getSortOrder()).displayOrder(e.getDisplayOrder()).altText(e.getAltText())
                .uploadedBy(e.getUploadedBy()).createdAt(e.getCreatedAt())
                .build();
    }

    public RoomImageJpaEntity toEntity(RoomImage d) {
        if (d == null) return null;
        return RoomImageJpaEntity.builder()
                .id(d.getId()).roomId(d.getRoomId()).imageUrl(d.getImageUrl()).storageKey(d.getStorageKey())
                .thumbKey(d.getThumbKey()).mediumKey(d.getMediumKey()).largeKey(d.getLargeKey())
                .originalFilename(d.getOriginalFilename()).contentType(d.getContentType()).sizeBytes(d.getSizeBytes())
                .width(d.getWidth()).height(d.getHeight()).cover(d.isCover()).primary(d.isPrimary())
                .sortOrder(d.getSortOrder()).displayOrder(d.getDisplayOrder()).altText(d.getAltText())
                .uploadedBy(d.getUploadedBy()).createdAt(d.getCreatedAt())
                .build();
    }

    public RoomPrice toDomain(RoomPriceJpaEntity e) {
        if (e == null) return null;
        return RoomPrice.builder()
                .id(e.getId()).roomId(e.getRoomId()).label(e.getLabel()).price(e.getPrice()).unit(e.getUnit())
                .billingMode(e.getBillingMode()).metered(e.isMetered()).meterType(e.getMeterType())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public RoomPriceJpaEntity toEntity(RoomPrice d) {
        if (d == null) return null;
        return RoomPriceJpaEntity.builder()
                .id(d.getId()).roomId(d.getRoomId()).label(d.getLabel()).price(d.getPrice()).unit(d.getUnit())
                .billingMode(d.getBillingMode()).metered(d.isMetered()).meterType(d.getMeterType())
                .createdAt(d.getCreatedAt())
                .build();
    }

    public RoomStatusHistory toDomain(RoomStatusHistoryJpaEntity e) {
        if (e == null) return null;
        return RoomStatusHistory.builder()
                .id(e.getId()).roomId(e.getRoomId()).fromStatus(e.getFromStatus()).toStatus(e.getToStatus())
                .actorId(e.getActorId()).actorRole(e.getActorRole()).reason(e.getReason()).createdAt(e.getCreatedAt())
                .build();
    }

    public RoomStatusHistoryJpaEntity toEntity(RoomStatusHistory d) {
        if (d == null) return null;
        return RoomStatusHistoryJpaEntity.builder()
                .id(d.getId()).roomId(d.getRoomId()).fromStatus(d.getFromStatus()).toStatus(d.getToStatus())
                .actorId(d.getActorId()).actorRole(d.getActorRole()).reason(d.getReason()).createdAt(d.getCreatedAt())
                .build();
    }

    public Room toDomain(RoomJpaEntity e) {
        if (e == null) return null;
        Set<Amenity> amenities = e.getAmenities() == null ? new HashSet<>()
                : e.getAmenities().stream().map(this::toDomain).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<RoomImage> images = e.getImages() == null ? new HashSet<>()
                : e.getImages().stream().map(this::toDomain).collect(Collectors.toCollection(LinkedHashSet::new));
        return Room.builder()
                .id(e.getId()).roomUuid(e.getRoomUuid()).landlordId(e.getLandlordId()).landlordName(e.getLandlordName())
                .landlordPhone(e.getLandlordPhone()).roomTypeId(e.getRoomTypeId()).roomTypeCode(e.getRoomTypeCode())
                .title(e.getTitle()).titleSearch(e.getTitleSearch()).slug(e.getSlug()).description(e.getDescription())
                .address(e.getAddress()).provinceCode(e.getProvinceCode()).districtCode(e.getDistrictCode())
                .wardCode(e.getWardCode()).wardId(e.getWardId()).latitude(e.getLatitude()).longitude(e.getLongitude())
                .area(e.getArea()).floor(e.getFloor()).bedrooms(e.getBedrooms()).bathrooms(e.getBathrooms())
                .maxOccupants(e.getMaxOccupants()).availableSpots(e.getAvailableSpots()).totalSpots(e.getTotalSpots())
                .price(e.getPrice()).priceUnit(e.getPriceUnit()).deposit(e.getDeposit())
                .electricityCost(e.getElectricityCost()).waterCost(e.getWaterCost()).internetCost(e.getInternetCost())
                .serviceCost(e.getServiceCost()).status(e.getStatus()).publicationStatus(e.getPublicationStatus())
                .occupancyStatus(e.getOccupancyStatus()).genderPreference(e.getGenderPreference()).furnishing(e.getFurnishing())
                .approved(e.isApproved()).allowPets(e.isAllowPets()).allowCooking(e.isAllowCooking())
                .curfew(e.getCurfew()).houseRules(e.getHouseRules()).verifiedOwnership(e.isVerifiedOwnership())
                .verifiedAt(e.getVerifiedAt()).availableDate(e.getAvailableDate()).minLeaseMonths(e.getMinLeaseMonths())
                .ratingAvg(e.getRatingAvg()).reviewCount(e.getReviewCount()).ratingCleanliness(e.getRatingCleanliness())
                .ratingLocation(e.getRatingLocation()).ratingPriceQuality(e.getRatingPriceQuality())
                .ratingSecurity(e.getRatingSecurity()).viewCount(e.getViewCount()).bookmarkCount(e.getBookmarkCount())
                .version(e.getVersion()).publishedAt(e.getPublishedAt()).hiddenAt(e.getHiddenAt())
                .hiddenBy(e.getHiddenBy()).hiddenReason(e.getHiddenReason()).rejectedReason(e.getRejectedReason())
                .deletedAt(e.getDeletedAt()).amenities(amenities).images(images)
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    public RoomJpaEntity toEntity(Room d) {
        if (d == null) return null;
        Set<AmenityJpaEntity> amenities = d.getAmenities() == null ? new HashSet<>()
                : d.getAmenities().stream().map(this::toEntity).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<RoomImageJpaEntity> images = d.getImages() == null ? new HashSet<>()
                : d.getImages().stream().map(this::toEntity).collect(Collectors.toCollection(LinkedHashSet::new));
        return RoomJpaEntity.builder()
                .id(d.getId()).roomUuid(d.getRoomUuid()).landlordId(d.getLandlordId()).landlordName(d.getLandlordName())
                .landlordPhone(d.getLandlordPhone()).roomTypeId(d.getRoomTypeId()).roomTypeCode(d.getRoomTypeCode())
                .title(d.getTitle()).titleSearch(d.getTitleSearch()).slug(d.getSlug()).description(d.getDescription())
                .address(d.getAddress()).provinceCode(d.getProvinceCode()).districtCode(d.getDistrictCode())
                .wardCode(d.getWardCode()).wardId(d.getWardId()).latitude(d.getLatitude()).longitude(d.getLongitude())
                .area(d.getArea()).floor(d.getFloor()).bedrooms(d.getBedrooms()).bathrooms(d.getBathrooms())
                .maxOccupants(d.getMaxOccupants()).availableSpots(d.getAvailableSpots()).totalSpots(d.getTotalSpots())
                .price(d.getPrice()).priceUnit(d.getPriceUnit()).deposit(d.getDeposit())
                .electricityCost(d.getElectricityCost()).waterCost(d.getWaterCost()).internetCost(d.getInternetCost())
                .serviceCost(d.getServiceCost()).status(d.getStatus()).publicationStatus(d.getPublicationStatus())
                .occupancyStatus(d.getOccupancyStatus()).genderPreference(d.getGenderPreference()).furnishing(d.getFurnishing())
                .approved(d.isApproved()).allowPets(d.isAllowPets()).allowCooking(d.isAllowCooking())
                .curfew(d.getCurfew()).houseRules(d.getHouseRules()).verifiedOwnership(d.isVerifiedOwnership())
                .verifiedAt(d.getVerifiedAt()).availableDate(d.getAvailableDate()).minLeaseMonths(d.getMinLeaseMonths())
                .ratingAvg(d.getRatingAvg()).reviewCount(d.getReviewCount()).ratingCleanliness(d.getRatingCleanliness())
                .ratingLocation(d.getRatingLocation()).ratingPriceQuality(d.getRatingPriceQuality())
                .ratingSecurity(d.getRatingSecurity()).viewCount(d.getViewCount()).bookmarkCount(d.getBookmarkCount())
                .version(d.getVersion()).publishedAt(d.getPublishedAt()).hiddenAt(d.getHiddenAt())
                .hiddenBy(d.getHiddenBy()).hiddenReason(d.getHiddenReason()).rejectedReason(d.getRejectedReason())
                .deletedAt(d.getDeletedAt()).amenities(amenities).images(images)
                .createdAt(d.getCreatedAt()).updatedAt(d.getUpdatedAt())
                .build();
    }
}
