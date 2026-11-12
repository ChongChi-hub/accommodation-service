package vn.cozynest.listing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_uuid", unique = true, length = 36)
    private String roomUuid;

    @Column(name = "landlord_id", nullable = false)
    private Long landlordId;

    @Column(name = "landlord_name", nullable = false, length = 100)
    private String landlordName;

    @Column(name = "landlord_phone", length = 20)
    private String landlordPhone;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "room_type_code", nullable = false, length = 50)
    private String roomTypeCode;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "title_search", length = 255)
    private String titleSearch;

    @Column(unique = true, length = 300)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "province_code", nullable = false, length = 20)
    private String provinceCode;

    @Column(name = "district_code", length = 20)
    private String districtCode;

    @Column(name = "ward_code", length = 20)
    private String wardCode;

    @Column(name = "ward_id")
    private Long wardId;

    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private Double area;

    @Column(name = "floor")
    @Builder.Default
    private Integer floor = 2;

    @Column(name = "bedrooms")
    @Builder.Default
    private Integer bedrooms = 1;

    @Column(name = "bathrooms")
    @Builder.Default
    private Integer bathrooms = 1;

    @Column(name = "max_occupants", nullable = false)
    private Integer maxOccupants;

    @Column(name = "available_spots")
    private Integer availableSpots;

    @Column(name = "total_spots")
    private Integer totalSpots;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "price_unit", nullable = false, length = 16)
    @Builder.Default
    private String priceUnit = "ROOM";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deposit;

    @Column(name = "electricity_cost", precision = 10, scale = 2)
    private BigDecimal electricityCost;

    @Column(name = "water_cost", precision = 10, scale = 2)
    private BigDecimal waterCost;

    @Column(name = "internet_cost", precision = 10, scale = 2)
    private BigDecimal internetCost;

    @Column(name = "service_cost", precision = 10, scale = 2)
    private BigDecimal serviceCost;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "publication_status", nullable = false, length = 30)
    @Builder.Default
    private String publicationStatus = "PUBLISHED";

    @Column(name = "occupancy_status", nullable = false, length = 30)
    @Builder.Default
    private String occupancyStatus = "AVAILABLE";

    @Column(name = "gender_preference", length = 30)
    @Builder.Default
    private String genderPreference = "ALL";

    @Column(name = "furnishing", length = 30)
    @Builder.Default
    private String furnishing = "FULL";

    @Column(name = "is_approved", nullable = false)
    private boolean approved;

    @Column(name = "allow_pets", nullable = false)
    private boolean allowPets;

    @Column(name = "allow_cooking", nullable = false)
    private boolean allowCooking;

    @Column(name = "curfew", length = 100)
    @Builder.Default
    private String curfew = "Giờ giấc tự do, khóa vân tay riêng";

    @Column(name = "house_rules", columnDefinition = "TEXT")
    private String houseRules;

    @Column(name = "is_verified_ownership", nullable = false)
    @Builder.Default
    private boolean verifiedOwnership = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "available_date")
    private LocalDate availableDate;

    @Column(name = "min_lease_months")
    @Builder.Default
    private Integer minLeaseMonths = 6;

    @Column(name = "rating_avg")
    private Double ratingAvg;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "rating_cleanliness")
    @Builder.Default
    private Double ratingCleanliness = 4.9;

    @Column(name = "rating_location")
    @Builder.Default
    private Double ratingLocation = 4.8;

    @Column(name = "rating_price_quality")
    @Builder.Default
    private Double ratingPriceQuality = 4.7;

    @Column(name = "rating_security")
    @Builder.Default
    private Double ratingSecurity = 5.0;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "bookmark_count", nullable = false)
    @Builder.Default
    private Integer bookmarkCount = 0;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    @Column(name = "hidden_by", length = 20)
    private String hiddenBy;

    @Column(name = "hidden_reason", length = 500)
    private String hiddenReason;

    @Column(name = "rejected_reason", length = 500)
    private String rejectedReason;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToMany
    @JoinTable(name = "room_amenities", joinColumns = @JoinColumn(name = "room_id"), inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private Set<AmenityJpaEntity> amenities = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private Set<RoomImageJpaEntity> images = new HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
