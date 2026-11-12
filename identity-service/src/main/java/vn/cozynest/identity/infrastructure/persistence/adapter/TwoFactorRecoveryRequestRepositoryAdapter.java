package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.TwoFactorRecoveryRequest;
import vn.cozynest.identity.domain.repository.TwoFactorRecoveryRequestRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.TwoFactorRecoveryRequestJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataTwoFactorRecoveryRepository;

import java.util.Optional;

@Component
public class TwoFactorRecoveryRequestRepositoryAdapter implements TwoFactorRecoveryRequestRepository {

    private final SpringDataTwoFactorRecoveryRepository repository;

    public TwoFactorRecoveryRequestRepositoryAdapter(SpringDataTwoFactorRecoveryRepository repository) {
        this.repository = repository;
    }

    @Override
    public TwoFactorRecoveryRequest save(TwoFactorRecoveryRequest r) {
        TwoFactorRecoveryRequestJpaEntity saved = repository.save(TwoFactorRecoveryRequestJpaEntity.builder()
                .id(r.getId())
                .targetUserId(r.getTargetUserId())
                .requestedBy(r.getRequestedBy())
                .approvedBy(r.getApprovedBy())
                .status(r.getStatus())
                .reason(r.getReason())
                .createdAt(r.getCreatedAt())
                .resolvedAt(r.getResolvedAt())
                .build());
        return toDomain(saved);
    }

    @Override
    public Optional<TwoFactorRecoveryRequest> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    private TwoFactorRecoveryRequest toDomain(TwoFactorRecoveryRequestJpaEntity e) {
        return new TwoFactorRecoveryRequest(
                e.getId(), e.getTargetUserId(), e.getRequestedBy(), e.getApprovedBy(),
                e.getStatus(), e.getReason(), e.getCreatedAt(), e.getResolvedAt()
        );
    }
}

