package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.identity.domain.model.RefreshToken;
import vn.cozynest.identity.domain.repository.RefreshTokenRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataRefreshTokenRepository;

import java.time.Instant;
import java.util.Optional;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {
    private final SpringDataRefreshTokenRepository springDataRefreshTokenRepository;

    public RefreshTokenRepositoryAdapter(SpringDataRefreshTokenRepository springDataRefreshTokenRepository) {
        this.springDataRefreshTokenRepository = springDataRefreshTokenRepository;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return springDataRefreshTokenRepository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity entity = toEntity(refreshToken);
        RefreshTokenJpaEntity saved = springDataRefreshTokenRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(String userId) {
        springDataRefreshTokenRepository.revokeAllByUserId(userId, Instant.now());
    }

    @Override
    @Transactional
    public void revokeAllExceptByUserId(String userId, String keepTokenHash) {
        springDataRefreshTokenRepository.revokeAllExceptByUserId(userId, keepTokenHash, Instant.now());
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return new RefreshToken(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getDeviceInfo(),
                entity.getIpAddress(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getReplacedBy(),
                entity.getCreatedAt()
        );
    }

    private RefreshTokenJpaEntity toEntity(RefreshToken domain) {
        return RefreshTokenJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .tokenHash(domain.getTokenHash())
                .deviceInfo(domain.getDeviceInfo())
                .ipAddress(domain.getIpAddress())
                .expiresAt(domain.getExpiresAt())
                .revokedAt(domain.getRevokedAt())
                .replacedBy(domain.getReplacedBy())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

