package vn.cozynest.identity.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FaceStatusResponse {
    private String status;
    private String provider;
    private boolean livenessClientFlag;
    private int attemptNo;
    private int remainingAttempts;
    private boolean canSubmit;
    private Instant consentAt;
    private String consentVersion;
    private String rejectReasonCode;
    private String rejectReasonNote;
    private Instant submittedAt;
    private Instant reviewedAt;
}

