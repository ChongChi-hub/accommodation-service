package vn.cozynest.identity.domain.model;

import vn.cozynest.identity.domain.exception.IdentityDomainException;

import java.time.Instant;
import java.util.UUID;

public class LandlordProfile {
    private final String id;
    private final String userId;
    private LandlordApprovalStatus approvalStatus;
    private final Instant appliedAt;
    private Instant approvedAt;
    private String reviewedBy;
    private Instant reviewedAt;
    private String rejectReasonCode;
    private String rejectReasonNote;
    private Long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public LandlordProfile(
            String id,
            String userId,
            LandlordApprovalStatus approvalStatus,
            Instant appliedAt,
            Instant approvedAt,
            String reviewedBy,
            Instant reviewedAt,
            String rejectReasonCode,
            String rejectReasonNote,
            Long version,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.userId = userId;
        this.approvalStatus = approvalStatus != null ? approvalStatus : LandlordApprovalStatus.AWAITING_KYC;
        this.appliedAt = appliedAt != null ? appliedAt : Instant.now();
        this.approvedAt = approvedAt;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.rejectReasonCode = rejectReasonCode;
        this.rejectReasonNote = rejectReasonNote;
        this.version = version != null ? version : 0L;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static LandlordProfile createNew(String userId) {
        Instant now = Instant.now();
        return new LandlordProfile(
                UUID.randomUUID().toString(),
                userId,
                LandlordApprovalStatus.AWAITING_KYC,
                now,
                null,
                null,
                null,
                null,
                null,
                0L,
                now,
                now
        );
    }

    /**
     * State transition: AWAITING_KYC -> PENDING_REVIEW or REJECTED -> PENDING_REVIEW
     */
    public void submitKyc() {
        if (this.approvalStatus == LandlordApprovalStatus.APPROVED) {
            throw new IdentityDomainException("Hồ sơ chủ trọ đã được phê duyệt", "ERR_LANDLORD_INVALID_STATE_TRANSITION");
        }
        if (this.approvalStatus == LandlordApprovalStatus.PENDING_REVIEW) {
            // Already pending review, no-op or re-submit
            this.updatedAt = Instant.now();
            return;
        }
        if (this.approvalStatus != LandlordApprovalStatus.AWAITING_KYC && this.approvalStatus != LandlordApprovalStatus.REJECTED) {
            throw new IdentityDomainException("Không thể nộp hồ sơ từ trạng thái: " + this.approvalStatus, "ERR_LANDLORD_INVALID_STATE_TRANSITION");
        }
        this.approvalStatus = LandlordApprovalStatus.PENDING_REVIEW;
        this.rejectReasonCode = null;
        this.rejectReasonNote = null;
        this.updatedAt = Instant.now();
    }

    /**
     * State transition: PENDING_REVIEW -> APPROVED
     */
    public void approve(String reviewerId) {
        if (this.approvalStatus == LandlordApprovalStatus.APPROVED) {
            return; // Idempotent
        }
        if (this.approvalStatus != LandlordApprovalStatus.PENDING_REVIEW) {
            throw new IdentityDomainException("Chỉ có thể phê duyệt hồ sơ đang chờ xét duyệt (PENDING_REVIEW). Trạng thái hiện tại: " + this.approvalStatus, "ERR_LANDLORD_INVALID_STATE_TRANSITION");
        }
        this.approvalStatus = LandlordApprovalStatus.APPROVED;
        this.approvedAt = Instant.now();
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
        this.rejectReasonCode = null;
        this.rejectReasonNote = null;
        this.updatedAt = Instant.now();
    }

    /**
     * State transition: PENDING_REVIEW -> REJECTED
     */
    public void reject(String reviewerId, String reasonCode, String reasonNote) {
        if (this.approvalStatus == LandlordApprovalStatus.APPROVED) {
            throw new IdentityDomainException("Không thể từ chối hồ sơ chủ trọ đã được phê duyệt", "ERR_LANDLORD_INVALID_STATE_TRANSITION");
        }
        if (this.approvalStatus != LandlordApprovalStatus.PENDING_REVIEW) {
            throw new IdentityDomainException("Chỉ có thể từ chối hồ sơ đang chờ xét duyệt (PENDING_REVIEW). Trạng thái hiện tại: " + this.approvalStatus, "ERR_LANDLORD_INVALID_STATE_TRANSITION");
        }
        this.approvalStatus = LandlordApprovalStatus.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
        this.rejectReasonCode = reasonCode;
        this.rejectReasonNote = reasonNote;
        this.updatedAt = Instant.now();
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public LandlordApprovalStatus getApprovalStatus() { return approvalStatus; }
    public Instant getAppliedAt() { return appliedAt; }
    public Instant getApprovedAt() { return approvedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getRejectReasonCode() { return rejectReasonCode; }
    public String getRejectReasonNote() { return rejectReasonNote; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setVersion(Long version) { this.version = version; }
}

