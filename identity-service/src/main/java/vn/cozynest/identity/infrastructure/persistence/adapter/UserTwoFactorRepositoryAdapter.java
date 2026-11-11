package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.UserTwoFactor;
import vn.cozynest.identity.domain.repository.UserTwoFactorRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.UserTwoFactorJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataUserTwoFactorRepository;

import java.util.Optional;

@Component
public class UserTwoFactorRepositoryAdapter implements UserTwoFactorRepository {
    private final SpringDataUserTwoFactorRepository repository;

    public UserTwoFactorRepositoryAdapter(SpringDataUserTwoFactorRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserTwoFactor save(UserTwoFactor domain) {
        UserTwoFactorJpaEntity entity = UserTwoFactorJpaEntity.builder()
                .userId(domain.getUserId())
                .totpSecret(domain.getTotpSecret())
                .enabled(domain.isEnabled())
                .failedAttempts(domain.getFailedAttempts())
                .lockedUntil(domain.getLockedUntil())
                .backupCodesJson(domain.getBackupCodesJson())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
        UserTwoFactorJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<UserTwoFactor> findByUserId(String userId) {
        return repository.findById(userId).map(this::toDomain);
    }

    @Override
    public void deleteByUserId(String userId) {
        repository.deleteById(userId);
    }

    private UserTwoFactor toDomain(UserTwoFactorJpaEntity entity) {
        return new UserTwoFactor(
                entity.getUserId(),
                entity.getTotpSecret(),
                entity.isEnabled(),
                entity.getFailedAttempts(),
                entity.getLockedUntil(),
                entity.getBackupCodesJson(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

