package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.LandlordProfile;

import java.util.Optional;

public interface LandlordProfileRepository {
    Optional<LandlordProfile> findById(String id);
    Optional<LandlordProfile> findByUserId(String userId);
    LandlordProfile save(LandlordProfile landlordProfile);
    boolean existsByUserId(String userId);
}

