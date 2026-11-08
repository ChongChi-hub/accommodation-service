package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.cozynest.identity.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;

import java.util.Optional;

@Repository
public interface SpringDataPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenJpaEntity, String> {

    Optional<PasswordResetTokenJpaEntity> findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(String userId);

    @Modifying
    @Query("UPDATE PasswordResetTokenJpaEntity t SET t.usedAt = CURRENT_TIMESTAMP WHERE t.userId = :userId AND t.usedAt IS NULL")
    void invalidateAllActiveTokens(String userId);
}
