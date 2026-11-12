package vn.cozynest.listing.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandlordSnapshot {

    private Long id;
    private Long landlordId;
    private String name;
    private String phone;
    private String avatarUrl;
    private String email;

    @Builder.Default
    private boolean verified = true;

    @Builder.Default
    private Double ratingAvg = 4.9;

    @Builder.Default
    private Integer reviewCount = 28;

    @Builder.Default
    private Integer responseRatePercent = 99;

    @Builder.Default
    private Integer responseTimeMinutes = 10;

    @Builder.Default
    private Integer roomCount = 6;

    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
