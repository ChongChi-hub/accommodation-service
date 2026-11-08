package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.FaceVerificationFrame;

import java.util.List;
import java.util.Optional;

public interface FaceVerificationFrameRepository {
    FaceVerificationFrame save(FaceVerificationFrame frame);
    List<FaceVerificationFrame> saveAll(List<FaceVerificationFrame> frames);
    Optional<FaceVerificationFrame> findById(String id);
    List<FaceVerificationFrame> findByFaceVerificationId(String faceVerificationId);
    List<FaceVerificationFrame> findActiveByFaceVerificationId(String faceVerificationId);
    void deleteByFaceVerificationId(String faceVerificationId);
}

