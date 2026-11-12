package vn.cozynest.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public host storefront DTO strictly limited to non-sensitive listing metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostPublicProfileDto {
    private Long id;
    private Long landlordId;
    private String name;
    private String avatarUrl;
    private boolean verified;
    private Double ratingAvg;
    private Integer reviewCount;
    private Integer responseRatePercent;
    private Integer responseTimeMinutes;
    private Integer roomCount;
    private LocalDateTime joinedAt;
    private List<RoomDto> rooms;
}

