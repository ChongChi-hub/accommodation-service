package vn.cozynest.identity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.cozynest.identity.domain.model.LandlordApprovalStatus;
import vn.cozynest.identity.domain.model.LandlordProfile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandlordApplicationSummaryDto {
    private String status;
    private String rejectReasonCode;
    private String rejectReasonNote;
    private boolean canResubmit;

    public static LandlordApplicationSummaryDto fromDomain(LandlordProfile profile) {
        if (profile == null) return null;
        boolean canResubmit = profile.getApprovalStatus() == LandlordApprovalStatus.REJECTED;
        return LandlordApplicationSummaryDto.builder()
                .status(profile.getApprovalStatus().name())
                .rejectReasonCode(profile.getRejectReasonCode())
                .rejectReasonNote(profile.getRejectReasonNote())
                .canResubmit(canResubmit)
                .build();
    }
}

