package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.RiskProfile;
import vn.cozynest.billing.domain.repository.RiskProfileRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.RiskProfileJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataRiskProfileRepository;

import java.util.List;
import java.util.Optional;

@Component
public class RiskProfileRepositoryAdapter implements RiskProfileRepository {
    private final SpringDataRiskProfileRepository repository;

    public RiskProfileRepositoryAdapter(SpringDataRiskProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public RiskProfile save(RiskProfile domain) {
        RiskProfileJpaEntity saved = repository.save(toEntity(domain));
        return toDomain(saved);
    }

    @Override
    public Optional<RiskProfile> findByUserId(String userId) {
        return repository.findById(userId).map(this::toDomain);
    }

    @Override
    public List<RiskProfile> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private RiskProfile toDomain(RiskProfileJpaEntity entity) {
        return new RiskProfile(
                entity.getUserId(), entity.getDailyLimit(), entity.getDailyWithdrawnAmount(),
                entity.getLastWithdrawalDate(), entity.getVelocityCount(), entity.isFrozen(),
                entity.getFreezeReason(), entity.getBankChangedAt(), entity.isBlacklisted(),
                entity.getBlacklistReason(), entity.getUpdatedAt());
    }

    private RiskProfileJpaEntity toEntity(RiskProfile domain) {
        return RiskProfileJpaEntity.builder()
                .userId(domain.getUserId()).dailyLimit(domain.getDailyLimit())
                .dailyWithdrawnAmount(domain.getDailyWithdrawnAmount())
                .lastWithdrawalDate(domain.getLastWithdrawalDate())
                .velocityCount(domain.getVelocityCount()).frozen(domain.isFrozen())
                .freezeReason(domain.getFreezeReason()).bankChangedAt(domain.getBankChangedAt())
                .blacklisted(domain.isBlacklisted()).blacklistReason(domain.getBlacklistReason())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
