package vn.cozynest.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDto {
    private Long id;
    private Long landlordId;
    private String landlordName;
    private String landlordPhone;
    private String roomTypeCode;
    private String title;
    private String description;
    private String address;
    private String provinceCode;
    private String districtCode;
    private String wardCode;
    private Long wardId;
    private Double latitude;
    private Double longitude;
    private Double area;
    private Integer maxOccupants;
    private BigDecimal price;
    private BigDecimal deposit;
    private BigDecimal electricityCost;
    private BigDecimal waterCost;
    private BigDecimal internetCost;
    private BigDecimal serviceCost;
    private String status;
    private boolean approved;
    private boolean allowPets;
    private boolean allowCooking;
    private Double ratingAvg;
    private Integer reviewCount;
    private Integer bookmarkCount;
    private Boolean isBookmarked;
    private String primaryImageUrl;
    private List<String> imageUrls;
    private List<AmenityDto> amenities;
    private String slug;
    private String publicationStatus;
    private String occupancyStatus;
    private String genderPreference;
    private String furnishing;
    private String provinceName;
    private String wardName;
    private String roomTypeName;
    private RoomImageDto coverImage;
    private List<AmenityDto> highlightAmenities;
    @Builder.Default
    private Boolean isLandlordVerified = true;
}

