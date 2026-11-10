package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.RiskProfile;

import java.util.Optional;

public interface RiskProfileRepository {
    RiskProfile save(RiskProfile profile);
    Optional<RiskProfile> findByUserId(String userId);
}

