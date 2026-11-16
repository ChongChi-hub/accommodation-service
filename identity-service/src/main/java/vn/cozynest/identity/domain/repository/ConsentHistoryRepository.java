package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.ConsentHistory;

import java.util.List;

public interface ConsentHistoryRepository {
    ConsentHistory save(ConsentHistory consent);
    List<ConsentHistory> findByUserIdOrderByCreatedAtDesc(String userId);
}
