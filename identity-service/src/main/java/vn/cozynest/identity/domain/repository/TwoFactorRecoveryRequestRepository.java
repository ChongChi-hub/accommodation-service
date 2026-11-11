package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.TwoFactorRecoveryRequest;

import java.util.Optional;

public interface TwoFactorRecoveryRequestRepository {
    TwoFactorRecoveryRequest save(TwoFactorRecoveryRequest request);
    Optional<TwoFactorRecoveryRequest> findById(String id);
}

