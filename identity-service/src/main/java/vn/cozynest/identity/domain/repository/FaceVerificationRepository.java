package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.FaceVerification;
import vn.cozynest.identity.domain.model.FaceVerificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FaceVerificationRepository {
    FaceVerification save(FaceVerification verification);
    Optional<FaceVerification> findById(String id);
    Optional<FaceVerification> findByUserId(String userId);
    Optional<FaceVerification> findByKycId(String kycId);
    int countAttemptsInLast24Hours(String userId, Instant since);
    List<FaceVerification> findEligibleForPurge(Instant decidedBefore, List<FaceVerificationStatus> statuses);
}

