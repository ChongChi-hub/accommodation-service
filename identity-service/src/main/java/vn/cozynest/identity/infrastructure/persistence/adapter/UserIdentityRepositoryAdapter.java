package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.UserIdentity;
import vn.cozynest.identity.domain.repository.UserIdentityRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.UserIdentityJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataUserIdentityRepository;

import java.util.List;
import java.util.Optional;

@Component
public class UserIdentityRepositoryAdapter implements UserIdentityRepository {

    private final SpringDataUserIdentityRepository repository;

    public UserIdentityRepositoryAdapter(SpringDataUserIdentityRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserIdentity save(UserIdentity identity) {
        UserIdentityJpaEntity entity = toEntity(identity);
        UserIdentityJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<UserIdentity> findByProviderAndProviderSubject(String provider, String providerSubject) {
        return repository.findByProviderAndProviderSubject(provider, providerSubject).map(this::toDomain);
    }

    @Override
    public Optional<UserIdentity> findByUserIdAndProvider(String userId, String provider) {
        return repository.findByUserIdAndProvider(userId, provider).map(this::toDomain);
    }

    @Override
    public List<UserIdentity> findAllByUserId(String userId) {
        return repository.findAllByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteByUserIdAndProvider(String userId, String provider) {
        repository.deleteByUserIdAndProvider(userId, provider);
    }

    @Override
    public boolean existsByProviderAndProviderSubject(String provider, String providerSubject) {
        return repository.existsByProviderAndProviderSubject(provider, providerSubject);
    }

    @Override
    public int countByUserId(String userId) {
        return repository.countByUserId(userId);
    }

    private UserIdentity toDomain(UserIdentityJpaEntity e) {
        return new UserIdentity(
                e.getId(), e.getUserId(), e.getProvider(), e.getProviderSubject(),
                e.getEmailAtLink(), e.isEmailVerified(), e.getDisplayName(),
                e.getPictureUrl(), e.getLocale(), e.getLinkedAt(),
                e.getLastLoginAt(), e.getCreatedAt()
        );
    }

    private UserIdentityJpaEntity toEntity(UserIdentity d) {
        return UserIdentityJpaEntity.builder()
                .id(d.getId())
                .userId(d.getUserId())
                .provider(d.getProvider())
                .providerSubject(d.getProviderSubject())
                .emailAtLink(d.getEmailAtLink())
                .emailVerified(d.isEmailVerified())
                .displayName(d.getDisplayName())
                .pictureUrl(d.getPictureUrl())
                .locale(d.getLocale())
                .linkedAt(d.getLinkedAt())
                .lastLoginAt(d.getLastLoginAt())
                .createdAt(d.getCreatedAt())
                .build();
    }
}

