package vn.toam.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomSummaryDto {

    private long total;
    private long published;
    private long hidden;
    private long pendingReview;
    private long draft;
    private long rejected;
    private long rented;
}

