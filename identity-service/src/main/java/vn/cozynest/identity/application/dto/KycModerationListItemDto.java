package vn.cozynest.identity.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class KycModerationListItemDto {
    private String id;
    private String userId;
    private String userEmail;
    private String userFullName;
    private String fullNameOnId;
    private String idNumberMasked;
    private String status;
    private Instant submittedAt;
    private Instant reviewedAt;
    private String reviewedBy;
    private String rejectReasonCode;
    private int attemptCount;
}

