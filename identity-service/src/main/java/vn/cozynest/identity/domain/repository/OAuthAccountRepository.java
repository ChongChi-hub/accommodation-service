package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.OAuthAccount;

import java.util.Optional;

public interface OAuthAccountRepository {
    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
    OAuthAccount save(OAuthAccount account);
    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);
}

