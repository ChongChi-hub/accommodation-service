package vn.toam.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.toam.identity.infrastructure.persistence.entity.KycAccessLogJpaEntity;

import java.util.List;

@Repository
public interface SpringDataKycAccessLogRepository extends JpaRepository<KycAccessLogJpaEntity, String> {

    List<KycAccessLogJpaEntity> findByVerificationIdOrderByCreatedAtDesc(String verificationId);
}

