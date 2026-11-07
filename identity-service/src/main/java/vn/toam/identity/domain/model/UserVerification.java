package vn.toam.identity.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class UserVerification {

    private final String id;
    private final String userId;
    private String fullNameOnId;
    private LocalDate dateOfBirth;
    private String idNumberEnc;
    private String idNumberHash;
    private String idNumberLast4;
    private LocalDate issueDate;
    private String issuePlace;
    private String frontImageKey;
    private String backImageKey;
    private String selfieKey;
    private KycStatus status;
    private Instant consentAt;
    private String consentPolicyVersion;
    private Instant submittedAt;
    private Instant reviewedAt;
    private String reviewedBy;
    private KycRejectReasonCode rejectReasonCode;
    private String rejectReasonNote;
    private int attemptCount;
    private Long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public UserVerification(String id, String userId, String fullNameOnId, LocalDate dateOfBirth,
                            String idNumberEnc, String idNumberHash, String idNumberLast4,
                            LocalDate issueDate, String issuePlace, String frontImageKey,
                            String backImageKey, String selfieKey, KycStatus status,
                            Instant consentAt, String consentPolicyVersion, Instant submittedAt,
                            Instant reviewedAt, String reviewedBy, KycRejectReasonCode rejectReasonCode,
                            String rejectReasonNote, int attemptCount, Long version,
                            Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.userId = userId;
        this.fullNameOnId = fullNameOnId;
        this.dateOfBirth = dateOfBirth;
        this.idNumberEnc = idNumberEnc;
        this.idNumberHash = idNumberHash;
        this.idNumberLast4 = idNumberLast4;
        this.issueDate = issueDate;
        this.issuePlace = issuePlace;
        this.frontImageKey = frontImageKey;
        this.backImageKey = backImageKey;
        this.selfieKey = selfieKey;
        this.status = status != null ? status : KycStatus.PENDING;
        this.consentAt = consentAt;
        this.consentPolicyVersion = consentPolicyVersion;
        this.submittedAt = submittedAt != null ? submittedAt : Instant.now();
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
        this.rejectReasonCode = rejectReasonCode;
        this.rejectReasonNote = rejectReasonNote;
        this.attemptCount = Math.max(1, attemptCount);
        this.version = version != null ? version : 0L;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static UserVerification createInitialSubmission(
            String userId, String fullNameOnId, LocalDate dateOfBirth,
            String idNumberEnc, String idNumberHash, String idNumberLast4,
            LocalDate issueDate, String issuePlace, String frontImageKey,
            String backImageKey, String consentPolicyVersion) {
        Instant now = Instant.now();
        return new UserVerification(
                UUID.randomUUID().toString(),
                userId,
                fullNameOnId,
                dateOfBirth,
                idNumberEnc,
                idNumberHash,
                idNumberLast4,
                issueDate,
                issuePlace,
                frontImageKey,
                backImageKey,
                null,
                KycStatus.PENDING,
                now,
                consentPolicyVersion,
                now,
                null,
                null,
                null,
                null,
                1,
                0L,
                now,
                now
        );
    }

    public void approve(String reviewerId) {
        if (this.status != KycStatus.PENDING) {
            throw new IllegalStateException("Chi co the phe duyet ho so dang o trang thai PENDING");
        }
        if (this.userId.equals(reviewerId)) {
            throw new IllegalArgumentException("Nguoi kiem duyet khong duoc phep la chu so huu ho so");
        }
        this.status = KycStatus.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void reject(String reviewerId, KycRejectReasonCode reasonCode, String reasonNote) {
        if (this.status != KycStatus.PENDING) {
            throw new IllegalStateException("Chi co the tu choi ho so dang o trang thai PENDING");
        }
        if (this.userId.equals(reviewerId)) {
            throw new IllegalArgumentException("Nguoi kiem duyet khong duoc phep la chu so huu ho so");
        }
        if (reasonCode == null) {
            throw new IllegalArgumentException("Ly do tu choi la bat buoc");
        }
        if (reasonCode == KycRejectReasonCode.OTHER && (reasonNote == null || reasonNote.trim().length() < 10)) {
            throw new IllegalArgumentException("Ghi chu ly do khac phai tu 10 ky tu tro len");
        }
        this.status = KycStatus.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
        this.rejectReasonCode = reasonCode;
        this.rejectReasonNote = reasonNote != null ? reasonNote.trim() : null;
        this.updatedAt = Instant.now();
    }

    public void resubmit(String fullNameOnId, LocalDate dateOfBirth, String idNumberEnc,
                         String idNumberHash, String idNumberLast4, LocalDate issueDate,
                         String issuePlace, String frontImageKey, String backImageKey,
                         String consentPolicyVersion) {
        if (this.status != KycStatus.REJECTED) {
            throw new IllegalStateException("Chi co the nop lai ho so khi da bi REJECTED");
        }
        this.fullNameOnId = fullNameOnId;
        this.dateOfBirth = dateOfBirth;
        this.idNumberEnc = idNumberEnc;
        this.idNumberHash = idNumberHash;
        this.idNumberLast4 = idNumberLast4;
        this.issueDate = issueDate;
        this.issuePlace = issuePlace;
        this.frontImageKey = frontImageKey;
        this.backImageKey = backImageKey;
        this.consentPolicyVersion = consentPolicyVersion;
        this.consentAt = Instant.now();
        this.submittedAt = Instant.now();
        this.status = KycStatus.PENDING;
        this.reviewedBy = null;
        this.reviewedAt = null;
        this.rejectReasonCode = null;
        this.rejectReasonNote = null;
        this.attemptCount++;
        this.updatedAt = Instant.now();
    }

    public void purgeImages() {
        this.frontImageKey = "PURGED";
        this.backImageKey = "PURGED";
        this.updatedAt = Instant.now();
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getFullNameOnId() { return fullNameOnId; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getIdNumberEnc() { return idNumberEnc; }
    public String getIdNumberHash() { return idNumberHash; }
    public String getIdNumberLast4() { return idNumberLast4; }
    public LocalDate getIssueDate() { return issueDate; }
    public String getIssuePlace() { return issuePlace; }
    public String getFrontImageKey() { return frontImageKey; }
    public String getBackImageKey() { return backImageKey; }
    public String getSelfieKey() { return selfieKey; }
    public KycStatus getStatus() { return status; }
    public Instant getConsentAt() { return consentAt; }
    public String getConsentPolicyVersion() { return consentPolicyVersion; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public KycRejectReasonCode getRejectReasonCode() { return rejectReasonCode; }
    public String getRejectReasonNote() { return rejectReasonNote; }
    public int getAttemptCount() { return attemptCount; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setVersion(Long version) { this.version = version; }
}

