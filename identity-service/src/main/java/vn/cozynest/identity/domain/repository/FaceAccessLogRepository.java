package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.FaceAccessLog;

import java.util.List;

public interface FaceAccessLogRepository {
    FaceAccessLog save(FaceAccessLog log);
    List<FaceAccessLog> findByFaceVerificationId(String faceVerificationId);
}

