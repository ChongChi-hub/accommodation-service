package vn.toam.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.toam.identity.domain.model.KycRejectReasonCode;
import vn.toam.identity.domain.model.KycStatus;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "user_verifications")
@Getter
@Setter
public class UserVerificationJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "full_name_on_id", length = 150)
    private String fullNameOnId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "id_number_enc", length = 500)
    private String idNumberEnc;

    @Column(name = "id_number_hash", length = 64)
    private String idNumberHash;

    @Column(name = "id_number_last4", length = 4)
    private String idNumberLast4;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "issue_place", length = 150)
    private String issuePlace;

    @Column(name = "front_image_key", length = 255)
    private String frontImageKey;

    @Column(name = "back_image_key", length = 255)
    private String backImageKey;

    @Column(name = "selfie_key", length = 255)
    private String selfieKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private KycStatus status = KycStatus.PENDING;

    @Column(name = "consent_at")
    private Instant consentAt;

    @Column(name = "consent_policy_version", length = 20)
    private String consentPolicyVersion;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "reject_reason_code", length = 30)
    private KycRejectReasonCode rejectReasonCode;

    @Column(name = "reject_reason_note", columnDefinition = "TEXT")
    private String rejectReasonNote;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 1;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}

