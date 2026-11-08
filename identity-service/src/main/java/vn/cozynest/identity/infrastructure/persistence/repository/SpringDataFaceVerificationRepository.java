package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.cozynest.identity.domain.model.FaceVerificationStatus;
import vn.cozynest.identity.infrastructure.persistence.entity.FaceVerificationJpaEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataFaceVerificationRepository extends JpaRepository<FaceVerificationJpaEntity, String> {

    Optional<FaceVerificationJpaEntity> findByUserId(String userId);

    Optional<FaceVerificationJpaEntity> findByKycId(String kycId);

    @Query("SELECT COUNT(f) FROM FaceVerificationJpaEntity f WHERE f.userId = :userId AND f.createdAt >= :since")
    int countAttemptsSince(@Param("userId") String userId, @Param("since") Instant since);

    @Query("SELECT f FROM FaceVerificationJpaEntity f WHERE f.reviewedAt <= :decidedBefore AND f.status IN :statuses")
    List<FaceVerificationJpaEntity> findEligibleForPurge(
            @Param("decidedBefore") Instant decidedBefore,
            @Param("statuses") List<FaceVerificationStatus> statuses);
}

