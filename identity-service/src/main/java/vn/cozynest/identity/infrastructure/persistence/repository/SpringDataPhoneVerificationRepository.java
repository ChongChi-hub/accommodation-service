package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.cozynest.identity.infrastructure.persistence.entity.PhoneVerificationJpaEntity;

import java.time.Instant;
import java.util.Optional;

public interface SpringDataPhoneVerificationRepository extends JpaRepository<PhoneVerificationJpaEntity, String> {
    Optional<PhoneVerificationJpaEntity> findFirstByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    @Query("SELECT COUNT(p) FROM PhoneVerificationJpaEntity p WHERE p.phoneNumber = :phone AND p.createdAt >= :since")
    long countRecent(@Param("phone") String phone, @Param("since") Instant since);
}

