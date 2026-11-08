package vn.cozynest.identity.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class FaceModerationDetailDto {
    private String id;
    private String userId;
    private String kycId;
    private String userEmail;
    private String userFullName;
    private String fullNameOnId;
    private String idNumberMasked;
    private String status;
    private String provider;
    private boolean livenessClientFlag;
    private List<String> challengeActions;
    private int attemptNo;
    private Instant consentAt;
    private String consentVersion;
    private String reviewedBy;
    private Instant reviewedAt;
    private String rejectReasonCode;
    private String rejectReasonNote;
    private Long version;
    private List<FrameItem> frames;
    private List<AccessLogItem> accessLogs;

    @Getter
    @Builder
    public static class FrameItem {
        private String id;
        private String purpose;
        private long size;
        private String checksum;
        private Instant createdAt;
        private boolean isDeleted;
    }

    @Getter
    @Builder
    public static class AccessLogItem {
        private String actorId;
        private String actorRole;
        private String purpose;
        private String ipAddress;
        private Instant createdAt;
    }
}

