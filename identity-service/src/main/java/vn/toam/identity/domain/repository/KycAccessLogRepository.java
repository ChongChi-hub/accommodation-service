package vn.toam.identity.domain.repository;

import vn.toam.identity.domain.model.KycAccessLog;

import java.util.List;

public interface KycAccessLogRepository {

    List<KycAccessLog> findByVerificationIdOrderByCreatedAtDesc(String verificationId);

    KycAccessLog save(KycAccessLog log);
}

