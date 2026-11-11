package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.RiskProfile;

import java.util.List;
import java.util.Optional;

public interface RiskProfileRepository {
    RiskProfile save(RiskProfile profile);
    Optional<RiskProfile> findByUserId(String userId);
    default List<RiskProfile> findAll() { return List.of(); }
}
