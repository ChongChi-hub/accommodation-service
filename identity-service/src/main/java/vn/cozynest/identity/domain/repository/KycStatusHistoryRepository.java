package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.KycStatusHistory;

import java.util.List;

public interface KycStatusHistoryRepository {

    List<KycStatusHistory> findByVerificationIdOrderByCreatedAtAsc(String verificationId);

    KycStatusHistory save(KycStatusHistory history);
}

