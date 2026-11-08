package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.OAuthAccount;
import vn.cozynest.identity.domain.repository.OAuthAccountRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.OAuthAccountJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataOAuthAccountRepository;

import java.util.Optional;

@Component
public class OAuthAccountRepositoryAdapter implements OAuthAccountRepository {
    private final SpringDataOAuthAccountRepository repository;

    public OAuthAccountRepositoryAdapter(SpringDataOAuthAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId) {
        return repository.findByProviderAndProviderUserId(provider, providerUserId).map(this::toDomain);
    }

    @Override
    public OAuthAccount save(OAuthAccount account) {
        OAuthAccountJpaEntity saved = repository.save(toEntity(account));
        return toDomain(saved);
    }

    @Override
    public boolean existsByProviderAndProviderUserId(String provider, String providerUserId) {
        return repository.existsByProviderAndProviderUserId(provider, providerUserId);
    }

    private OAuthAccount toDomain(OAuthAccountJpaEntity entity) {
        return new OAuthAccount(
                entity.getId(),
                entity.getUserId(),
                entity.getProvider(),
                entity.getProviderUserId(),
                entity.getEmail(),
                entity.getCreatedAt()
        );
    }

    private OAuthAccountJpaEntity toEntity(OAuthAccount domain) {
        return OAuthAccountJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .provider(domain.getProvider())
                .providerUserId(domain.getProviderUserId())
                .email(domain.getEmail())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

