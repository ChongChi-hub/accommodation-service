package vn.cozynest.identity.domain.model;

import vn.cozynest.identity.domain.exception.IdentityDomainException;

import java.time.Instant;
import java.util.UUID;

public class FaceVerification {

    private final String id;
    private final String userId;
    private final String kycId;
    private String provider;
    private FaceVerificationStatus status;
    private boolean livenessClientFlag;
    private String challengeJson;
    private String challengeNonce;
    private Instant challengeExpiresAt;
    private int attemptNo;
    private Instant consentAt;
    private String consentVersion;
    private String reviewedBy;
    private Instant reviewedAt;
    private String rejectReasonCode;
    private String rejectReasonNote;
    private Long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public FaceVerification(
            String id, String userId, String kycId, String provider,
            FaceVerificationStatus status, boolean livenessClientFlag,
            String challengeJson, String challengeNonce, Instant challengeExpiresAt,
            int attemptNo, Instant consentAt, String consentVersion,
            String reviewedBy, Instant reviewedAt, String rejectReasonCode,
            String rejectReasonNote, Long version, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.userId = userId;
        this.kycId = kycId;
        this.provider = provider != null ? provider : "manual";
        this.status = status != null ? status : FaceVerificationStatus.PENDING;
        this.livenessClientFlag = livenessClientFlag;
        this.challengeJson = challengeJson;
        this.challengeNonce = challengeNonce;
        this.challengeExpiresAt = challengeExpiresAt;
        this.attemptNo = attemptNo > 0 ? attemptNo : 1;
        this.consentAt = consentAt;
        this.consentVersion = consentVersion;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.rejectReasonCode = rejectReasonCode;
        this.rejectReasonNote = rejectReasonNote;
        this.version = version != null ? version : 0L;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static FaceVerification createChallenge(
            String userId, String kycId, String provider, String challengeJson,
            String nonce, Instant expiresAt, int attemptNo, String consentVersion) {
        Instant now = Instant.now();
        return new FaceVerification(
                UUID.randomUUID().toString(), userId, kycId, provider,
                FaceVerificationStatus.PENDING, false, challengeJson, nonce,
                expiresAt, attemptNo, now, consentVersion, null, null, null, null, 0L, now, now
        );
    }

    public void updateChallenge(String challengeJson, String nonce, Instant expiresAt, int attemptNo) {
        this.challengeJson = challengeJson;
        this.challengeNonce = nonce;
        this.challengeExpiresAt = expiresAt;
        this.attemptNo = attemptNo;
        this.status = FaceVerificationStatus.PENDING;
        this.updatedAt = Instant.now();
    }

    public void completeSubmission(boolean livenessClientFlag) {
        if (this.status != FaceVerificationStatus.PENDING) {
            throw new IdentityDomainException("Hồ sơ xác thực khuôn mặt không ở trạng thái chờ nộp", "ERR_FACE_INVALID_STATE");
        }
        this.livenessClientFlag = livenessClientFlag;
        this.challengeNonce = null; // Single-use nonce consumed
        this.updatedAt = Instant.now();
    }

    public void pass(String reviewerId) {
        ensureCanDecide();
        this.status = FaceVerificationStatus.PASSED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void fail(String reviewerId, String reasonCode, String reasonNote) {
        ensureCanDecide();
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IdentityDomainException("Vui lòng cung cấp mã lý do từ chối khuôn mặt", "ERR_FACE_REASON_REQUIRED");
        }
        if ("KHAC".equalsIgnoreCase(reasonCode) && (reasonNote == null || reasonNote.trim().length() < 5)) {
            throw new IdentityDomainException("Lý do 'Khác' bắt buộc có ghi chú tối thiểu 5 ký tự", "ERR_FACE_NOTE_REQUIRED");
        }
        this.status = FaceVerificationStatus.FAILED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
        this.rejectReasonCode = reasonCode;
        this.rejectReasonNote = reasonNote;
        this.updatedAt = Instant.now();
    }

    public void markExpired() {
        if (this.status == FaceVerificationStatus.PENDING) {
            this.status = FaceVerificationStatus.EXPIRED;
            this.updatedAt = Instant.now();
        }
    }

    private void ensureCanDecide() {
        if (this.status == FaceVerificationStatus.PASSED || this.status == FaceVerificationStatus.FAILED) {
            throw new IdentityDomainException("Hồ sơ xác thực khuôn mặt đã có kết quả trước đó", "ERR_FACE_ALREADY_DECIDED");
        }
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getKycId() { return kycId; }
    public String getProvider() { return provider; }
    public FaceVerificationStatus getStatus() { return status; }
    public boolean isLivenessClientFlag() { return livenessClientFlag; }
    public String getChallengeJson() { return challengeJson; }
    public String getChallengeNonce() { return challengeNonce; }
    public Instant getChallengeExpiresAt() { return challengeExpiresAt; }
    public int getAttemptNo() { return attemptNo; }
    public Instant getConsentAt() { return consentAt; }
    public String getConsentVersion() { return consentVersion; }
    public String getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getRejectReasonCode() { return rejectReasonCode; }
    public String getRejectReasonNote() { return rejectReasonNote; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setVersion(Long version) { this.version = version; }
}

