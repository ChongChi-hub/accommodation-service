package vn.cozynest.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDetailDto {
    private Long id;
    private String roomUuid;
    private String slug;
    private String title;
    private String description;
    private String address;
    private String provinceCode;
    private String provinceName;
    private String wardCode;
    private String wardName;
    private Long wardId;
    private Double latitude;
    private Double longitude;

    private Long roomTypeId;
    private String roomTypeCode;
    private String roomTypeName;

    private Double area;
    private Integer floor;
    private Integer bedrooms;
    private Integer bathrooms;
    private Integer maxOccupants;

    private BigDecimal price;
    private String priceUnit;
    private BigDecimal deposit;
    private BigDecimal electricityCost;
    private BigDecimal waterCost;
    private BigDecimal internetCost;
    private BigDecimal serviceCost;
    private BigDecimal initialElectricityReading;
    private BigDecimal initialWaterReading;

    private String status;
    private String publicationStatus;
    private String occupancyStatus;
    private boolean approved;

    private boolean allowPets;
    private boolean allowCooking;
    private String genderPreference;
    private String furnishing;
    private String curfew;
    private LocalDate availableDate;
    private Integer minLeaseMonths;

    private Double ratingAvg;
    private Integer reviewCount;
    private Double ratingCleanliness;
    private Double ratingLocation;
    private Double ratingPriceQuality;
    private Double ratingSecurity;

    private Integer viewCount;
    private Integer bookmarkCount;
    private Boolean isBookmarked;
    private LocalDateTime publishedAt;

    private List<RoomImageDto> images;
    private List<AmenityDto> amenities;
    private List<RoomFeeDto> roomFees;
    private LandlordSnapshotDto landlord;
    private List<ReviewDto> reviews;
}

