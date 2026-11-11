package vn.cozynest.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class TwoFactorRecoveryRequest {
    private final String id;
    private final String targetUserId;
    private final String requestedBy;
    private String approvedBy;
    private String status;
    private final String reason;
    private final Instant createdAt;
    private Instant resolvedAt;

    public TwoFactorRecoveryRequest(String id, String targetUserId, String requestedBy,
                                    String approvedBy, String status, String reason,
                                    Instant createdAt, Instant resolvedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.targetUserId = targetUserId;
        this.requestedBy = requestedBy;
        this.approvedBy = approvedBy;
        this.status = status != null ? status : "PENDING";
        this.reason = reason;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.resolvedAt = resolvedAt;
    }

    public static TwoFactorRecoveryRequest request(String targetUserId, String requestedBy, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Ly do khoi phuc 2FA khong duoc de trong");
        }
        return new TwoFactorRecoveryRequest(UUID.randomUUID().toString(), targetUserId, requestedBy,
                null, "PENDING", reason.trim(), Instant.now(), null);
    }

    public void approve(String secondAdminId) {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("Yeu cau khoi phuc 2FA da duoc xu ly");
        }
        if (this.requestedBy.equals(secondAdminId)) {
            throw new IllegalStateException("Nguyen tac 2 quan tri vien: nguoi duyet phai khac nguoi tao yeu cau");
        }
        this.approvedBy = secondAdminId;
        this.status = "APPROVED";
        this.resolvedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getTargetUserId() { return targetUserId; }
    public String getRequestedBy() { return requestedBy; }
    public String getApprovedBy() { return approvedBy; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}

