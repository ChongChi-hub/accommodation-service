package vn.toam.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.toam.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, String> {
    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity r SET r.revokedAt = :now WHERE r.userId = :userId AND r.revokedAt IS NULL")
    void revokeAllByUserId(String userId, Instant now);

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity r SET r.revokedAt = :now WHERE r.userId = :userId AND r.revokedAt IS NULL AND (:keepTokenHash IS NULL OR r.tokenHash <> :keepTokenHash)")
    void revokeAllExceptByUserId(String userId, String keepTokenHash, Instant now);
}

