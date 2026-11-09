package vn.cozynest.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStatsDto {
    private long activeRooms;
    private long provincesWithRooms;
    private long verifiedLandlords;
    private long totalReviews;
    private long happyTenants;
}
