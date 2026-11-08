package vn.cozynest.identity.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class KycModerationDetailDto {
    private String id;
    private String userId;
    private String userEmail;
    private String userPhone;
    private String userFullName;
    private String fullNameOnId;
    private LocalDate dateOfBirth;
    private String idNumberPlain;
    private String idNumberLast4;
    private LocalDate issueDate;
    private String issuePlace;
    private String status;
    private Instant consentAt;
    private String consentPolicyVersion;
    private Instant submittedAt;
    private Instant reviewedAt;
    private String reviewedBy;
    private String rejectReasonCode;
    private String rejectReasonNote;
    private int attemptCount;
    private Long version;
    private List<StatusHistoryItem> statusHistory;
    private List<AccessLogItem> accessLogs;

    @Getter
    @Builder
    public static class StatusHistoryItem {
        private String fromStatus;
        private String toStatus;
        private String actorId;
        private String reason;
        private Instant createdAt;
    }

    @Getter
    @Builder
    public static class AccessLogItem {
        private String actorId;
        private String actorRole;
        private String action;
        private String ip;
        private Instant createdAt;
    }
}

