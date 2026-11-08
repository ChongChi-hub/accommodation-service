package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.cozynest.identity.infrastructure.persistence.entity.EmailVerificationTokenJpaEntity;

import java.util.Optional;

@Repository
public interface SpringDataEmailVerificationTokenRepository extends JpaRepository<EmailVerificationTokenJpaEntity, String> {

    @Query("SELECT e FROM EmailVerificationTokenJpaEntity e WHERE e.userId = :userId AND e.verifiedAt IS NULL ORDER BY e.createdAt DESC LIMIT 1")
    Optional<EmailVerificationTokenJpaEntity> findLatestActiveTokenByUserId(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationTokenJpaEntity e WHERE e.userId = :userId AND e.verifiedAt IS NULL")
    void invalidateAllActiveTokens(@Param("userId") String userId);
}

