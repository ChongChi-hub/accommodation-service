package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.cozynest.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;

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

    @Query("SELECT COUNT(r) > 0 FROM RefreshTokenJpaEntity r WHERE r.userId = :userId AND r.revokedAt IS NULL AND r.expiresAt > :now")
    boolean existsActiveTokenByUserId(String userId, Instant now);

    @Query("SELECT MAX(r.revokedAt) FROM RefreshTokenJpaEntity r WHERE r.userId = :userId")
    Optional<Instant> findLatestRevocationByUserId(String userId);
}

