package vn.toam.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.toam.identity.infrastructure.persistence.entity.KycStatusHistoryJpaEntity;

import java.util.List;

@Repository
public interface SpringDataKycStatusHistoryRepository extends JpaRepository<KycStatusHistoryJpaEntity, String> {

    List<KycStatusHistoryJpaEntity> findByVerificationIdOrderByCreatedAtAsc(String verificationId);
}

