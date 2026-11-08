package vn.cozynest.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.cozynest.identity.domain.model.FaceVerificationStatus;

import java.time.Instant;

@Entity
@Table(name = "face_verifications")
@Getter
@Setter
@NoArgsConstructor
public class FaceVerificationJpaEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "kyc_id", nullable = false, length = 36)
    private String kycId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider = "manual";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FaceVerificationStatus status = FaceVerificationStatus.PENDING;

    @Column(name = "liveness_client_flag", nullable = false)
    private boolean livenessClientFlag = false;

    @Column(name = "challenge_json", columnDefinition = "TEXT")
    private String challengeJson;

    @Column(name = "challenge_nonce", length = 64)
    private String challengeNonce;

    @Column(name = "challenge_expires_at")
    private Instant challengeExpiresAt;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo = 1;

    @Column(name = "consent_at")
    private Instant consentAt;

    @Column(name = "consent_version", length = 20)
    private String consentVersion;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}

