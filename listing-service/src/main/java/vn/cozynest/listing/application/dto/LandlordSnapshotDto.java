package vn.cozynest.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandlordSnapshotDto {
    private Long id;
    private Long landlordId;
    private String name;
    private String phone;
    private String avatarUrl;
    private String email;
    private boolean verified;
    private Double ratingAvg;
    private Integer reviewCount;
    private Integer responseRatePercent;
    private Integer responseTimeMinutes;
    private Integer roomCount;
    private LocalDateTime joinedAt;
}
