package vn.cozynest.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandlordRoomDetailDto {

    private Long id;
    private String roomUuid;
    private Long landlordId;
    private String landlordName;
    private String landlordPhone;
    private Long roomTypeId;
    private String roomTypeCode;
    private String title;
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
    private Integer maxOccupants;
    private BigDecimal price;
    private String priceUnit;
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
    private Integer viewCount;
    private LocalDateTime publishedAt;
    private LocalDateTime hiddenAt;
    private String hiddenBy;
    private String hiddenReason;
    private String rejectedReason;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<RoomImageDto> images;
}

