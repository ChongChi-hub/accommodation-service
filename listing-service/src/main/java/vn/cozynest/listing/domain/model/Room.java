package vn.cozynest.listing.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    private Long id;
    private String roomUuid;
    private Long landlordId;
    private String landlordName;
    private String landlordPhone;
    private Long roomTypeId;
    private String roomTypeCode;
    private String title;
    private String titleSearch;
    private String slug;
    private String description;
    private String address;
    private String provinceCode;
    private String districtCode;
    private String wardCode;
    private Long wardId;
    private Double latitude;
    private Double longitude;
    private Double area;

    @Builder.Default
    private Integer floor = 2;

    @Builder.Default
    private Integer bedrooms = 1;

    @Builder.Default
    private Integer bathrooms = 1;

    private Integer maxOccupants;
    private Integer availableSpots;
    private Integer totalSpots;
    private BigDecimal price;

    @Builder.Default
    private String priceUnit = "ROOM";

    private BigDecimal deposit;
    private BigDecimal electricityCost;
    private BigDecimal waterCost;
    private BigDecimal internetCost;
    private BigDecimal serviceCost;
    private String status;

    @Builder.Default
    private String publicationStatus = "PUBLISHED";

    @Builder.Default
    private String occupancyStatus = "AVAILABLE";

    @Builder.Default
    private String genderPreference = "ALL";

    @Builder.Default
    private String furnishing = "FULL";

    private boolean approved;
    private boolean allowPets;
    private boolean allowCooking;

    @Builder.Default
    private String curfew = "Giờ giấc tự do, khóa vân tay riêng";

    private String houseRules;

    @Builder.Default
    private boolean verifiedOwnership = false;

    private LocalDateTime verifiedAt;
    private LocalDate availableDate;

    @Builder.Default
    private Integer minLeaseMonths = 6;

    private Double ratingAvg;
    private Integer reviewCount;

    @Builder.Default
    private Double ratingCleanliness = 4.9;

    @Builder.Default
    private Double ratingLocation = 4.8;

    @Builder.Default
    private Double ratingPriceQuality = 4.7;

    @Builder.Default
    private Double ratingSecurity = 5.0;

    @Builder.Default
    private Integer viewCount = 0;

    @Builder.Default
    private Integer bookmarkCount = 0;

    @Builder.Default
    private Long version = 0L;

    private LocalDateTime publishedAt;
    private LocalDateTime hiddenAt;
    private String hiddenBy;
    private String hiddenReason;
    private String rejectedReason;
    private LocalDateTime deletedAt;

    @Builder.Default
    private Set<Amenity> amenities = new HashSet<>();

    @Builder.Default
    private Set<RoomImage> images = new HashSet<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isHidden() {
        return "HIDDEN".equalsIgnoreCase(status) || hiddenAt != null;
    }

    public boolean isPublished() {
        return "PUBLISHED".equalsIgnoreCase(status);
    }

    public boolean isRented() {
        return "RENTED".equalsIgnoreCase(status);
    }
}
