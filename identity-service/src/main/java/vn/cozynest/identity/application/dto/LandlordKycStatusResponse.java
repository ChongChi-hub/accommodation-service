package vn.cozynest.identity.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
public class LandlordKycStatusResponse {
    private String id;
    private String status;
    private String fullNameOnId;
    private LocalDate dateOfBirth;
    private String idNumberMasked;
    private LocalDate issueDate;
    private String issuePlace;
    private Instant submittedAt;
    private Instant reviewedAt;
    private String rejectReasonCode;
    private String rejectReasonNote;
    private int attemptCount;
    private int remainingAttempts;
    private boolean hasFrontImage;
    private boolean hasBackImage;
    private boolean canResubmit;
}

