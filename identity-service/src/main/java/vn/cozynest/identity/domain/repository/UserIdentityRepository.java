package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.UserIdentity;

import java.util.List;
import java.util.Optional;

public interface UserIdentityRepository {
    UserIdentity save(UserIdentity identity);
    Optional<UserIdentity> findByProviderAndProviderSubject(String provider, String providerSubject);
    Optional<UserIdentity> findByUserIdAndProvider(String userId, String provider);
    List<UserIdentity> findAllByUserId(String userId);
    void deleteByUserIdAndProvider(String userId, String provider);
    boolean existsByProviderAndProviderSubject(String provider, String providerSubject);
    int countByUserId(String userId);
}

