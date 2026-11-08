package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.identity.infrastructure.persistence.entity.OAuthAccountJpaEntity;

import java.util.Optional;

@Repository
public interface SpringDataOAuthAccountRepository extends JpaRepository<OAuthAccountJpaEntity, String> {
    Optional<OAuthAccountJpaEntity> findByProviderAndProviderUserId(String provider, String providerUserId);
    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);
}

