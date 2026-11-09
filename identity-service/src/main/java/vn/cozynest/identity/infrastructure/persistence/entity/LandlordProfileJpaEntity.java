package vn.cozynest.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.cozynest.identity.domain.model.LandlordApprovalStatus;
import vn.cozynest.identity.domain.model.LandlordProfile;

import java.time.Instant;

@Entity
@Table(name = "landlord_profiles")
@Getter
@Setter
@NoArgsConstructor
public class LandlordProfileJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false, unique = true)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 30, nullable = false)
    private LandlordApprovalStatus approvalStatus;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reject_reason_code", length = 50)
    private String rejectReasonCode;

    @Column(name = "reject_reason_note", columnDefinition = "TEXT")
    private String rejectReasonNote;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static LandlordProfileJpaEntity fromDomain(LandlordProfile domain) {
        LandlordProfileJpaEntity entity = new LandlordProfileJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setApprovalStatus(domain.getApprovalStatus());
        entity.setAppliedAt(domain.getAppliedAt());
        entity.setApprovedAt(domain.getApprovedAt());
        entity.setReviewedBy(domain.getReviewedBy());
        entity.setReviewedAt(domain.getReviewedAt());
        entity.setRejectReasonCode(domain.getRejectReasonCode());
        entity.setRejectReasonNote(domain.getRejectReasonNote());
        entity.setVersion(domain.getVersion());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public LandlordProfile toDomain() {
        return new LandlordProfile(
                this.id,
                this.userId,
                this.approvalStatus,
                this.appliedAt,
                this.approvedAt,
                this.reviewedBy,
                this.reviewedAt,
                this.rejectReasonCode,
                this.rejectReasonNote,
                this.version,
                this.createdAt,
                this.updatedAt
        );
    }
}

