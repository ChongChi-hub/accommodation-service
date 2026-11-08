package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.FaceVerification;
import vn.cozynest.identity.domain.model.FaceVerificationStatus;
import vn.cozynest.identity.domain.repository.FaceVerificationRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.FaceVerificationJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataFaceVerificationRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class FaceVerificationRepositoryAdapter implements FaceVerificationRepository {

    private final SpringDataFaceVerificationRepository repository;

    public FaceVerificationRepositoryAdapter(SpringDataFaceVerificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public FaceVerification save(FaceVerification v) {
        FaceVerificationJpaEntity entity = toEntity(v);
        FaceVerificationJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<FaceVerification> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<FaceVerification> findByUserId(String userId) {
        return repository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public Optional<FaceVerification> findByKycId(String kycId) {
        return repository.findByKycId(kycId).map(this::toDomain);
    }

    @Override
    public int countAttemptsInLast24Hours(String userId, Instant since) {
        return repository.countAttemptsSince(userId, since);
    }

    @Override
    public List<FaceVerification> findEligibleForPurge(Instant decidedBefore, List<FaceVerificationStatus> statuses) {
        return repository.findEligibleForPurge(decidedBefore, statuses).stream()
                .map(this::toDomain)
                .toList();
    }

    private FaceVerification toDomain(FaceVerificationJpaEntity e) {
        return new FaceVerification(
                e.getId(), e.getUserId(), e.getKycId(), e.getProvider(),
                e.getStatus(), e.isLivenessClientFlag(), e.getChallengeJson(),
                e.getChallengeNonce(), e.getChallengeExpiresAt(), e.getAttemptNo(),
                e.getConsentAt(), e.getConsentVersion(), e.getReviewedBy(),
                e.getReviewedAt(), e.getRejectReasonCode(), e.getRejectReasonNote(),
                e.getVersion(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    private FaceVerificationJpaEntity toEntity(FaceVerification d) {
        FaceVerificationJpaEntity e = new FaceVerificationJpaEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId());
        e.setKycId(d.getKycId());
        e.setProvider(d.getProvider());
        e.setStatus(d.getStatus());
        e.setLivenessClientFlag(d.isLivenessClientFlag());
        e.setChallengeJson(d.getChallengeJson());
        e.setChallengeNonce(d.getChallengeNonce());
        e.setChallengeExpiresAt(d.getChallengeExpiresAt());
        e.setAttemptNo(d.getAttemptNo());
        e.setConsentAt(d.getConsentAt());
        e.setConsentVersion(d.getConsentVersion());
        e.setReviewedBy(d.getReviewedBy());
        e.setReviewedAt(d.getReviewedAt());
        e.setRejectReasonCode(d.getRejectReasonCode());
        e.setRejectReasonNote(d.getRejectReasonNote());
        e.setVersion(d.getVersion());
        return e;
    }
}

