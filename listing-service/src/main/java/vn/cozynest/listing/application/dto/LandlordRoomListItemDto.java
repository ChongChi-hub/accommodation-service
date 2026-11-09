package vn.cozynest.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandlordRoomListItemDto {

    private Long id;
    private String roomUuid;
    private String title;
    private String address;
    private String shortAddress;
    private BigDecimal price;
    private String priceUnit;
    private BigDecimal deposit;
    private Double area;
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private Integer viewCount;
    private String rejectedReason;
    private String hiddenReason;
    private String hiddenBy;
    private String coverThumbUrl;
    private String coverMediumUrl;
    private Double ratingAvg;
    private Integer reviewCount;
}

