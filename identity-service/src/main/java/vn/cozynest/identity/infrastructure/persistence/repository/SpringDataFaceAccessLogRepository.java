package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.identity.infrastructure.persistence.entity.FaceAccessLogJpaEntity;

import java.util.List;

@Repository
public interface SpringDataFaceAccessLogRepository extends JpaRepository<FaceAccessLogJpaEntity, String> {

    List<FaceAccessLogJpaEntity> findByFaceVerificationIdOrderByCreatedAtDesc(String faceVerificationId);
}

