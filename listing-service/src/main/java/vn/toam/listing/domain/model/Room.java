package vn.toam.listing.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
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
public class Room {

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

    @Column(name = "max_occupants", nullable = false)
    private Integer maxOccupants;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

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

    @Column(name = "is_approved", nullable = false)
    private boolean approved;

    @Column(name = "allow_pets", nullable = false)
    private boolean allowPets;

    @Column(name = "allow_cooking", nullable = false)
    private boolean allowCooking;

    @Column(name = "rating_avg")
    private Double ratingAvg;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

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
    @JoinTable(
        name = "room_amenities",
        joinColumns = @JoinColumn(name = "room_id"),
        inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    @Builder.Default
    private Set<Amenity> amenities = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    @Builder.Default
    private Set<RoomImage> images = new HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
