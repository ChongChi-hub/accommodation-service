package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.cozynest.identity.infrastructure.persistence.entity.FaceVerificationFrameJpaEntity;

import java.util.List;

@Repository
public interface SpringDataFaceVerificationFrameRepository extends JpaRepository<FaceVerificationFrameJpaEntity, String> {

    List<FaceVerificationFrameJpaEntity> findByFaceVerificationId(String faceVerificationId);

    @Query("SELECT f FROM FaceVerificationFrameJpaEntity f WHERE f.faceVerificationId = :id AND f.deletedAt IS NULL")
    List<FaceVerificationFrameJpaEntity> findActiveByFaceVerificationId(@Param("id") String faceVerificationId);

    void deleteByFaceVerificationId(String faceVerificationId);
}

