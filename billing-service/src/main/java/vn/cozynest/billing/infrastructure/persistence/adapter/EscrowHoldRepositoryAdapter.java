package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.EscrowHold;
import vn.cozynest.billing.domain.repository.EscrowHoldRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.EscrowHoldJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataEscrowHoldRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class EscrowHoldRepositoryAdapter implements EscrowHoldRepository {
    private final SpringDataEscrowHoldRepository repository;

    public EscrowHoldRepositoryAdapter(SpringDataEscrowHoldRepository repository) {
        this.repository = repository;
    }

    @Override
    public EscrowHold save(EscrowHold h) {
        EscrowHoldJpaEntity saved = repository.save(EscrowHoldJpaEntity.builder()
                .id(h.getId()).contractId(h.getContractId()).tenantId(h.getTenantId())
                .landlordId(h.getLandlordId()).amount(h.getAmount()).status(h.getStatus())
                .sandboxMode(h.isSandboxMode()).inspectionEndsAt(h.getInspectionEndsAt())
                .disputeReason(h.getDisputeReason()).releasedAt(h.getReleasedAt())
                .createdAt(h.getCreatedAt()).updatedAt(h.getUpdatedAt())
                .build());
        return toDomain(saved);
    }

    @Override
    public Optional<EscrowHold> findByContractId(String contractId) {
        return repository.findByContractId(contractId).map(this::toDomain);
    }

    @Override
    public List<EscrowHold> findReleasableBefore(Instant cutoff) {
        return repository.findByStatusAndInspectionEndsAtBefore("HELD", cutoff).stream().map(this::toDomain).toList();
    }

    private EscrowHold toDomain(EscrowHoldJpaEntity e) {
        return new EscrowHold(e.getId(), e.getContractId(), e.getTenantId(), e.getLandlordId(),
                e.getAmount(), e.getStatus(), e.isSandboxMode(), e.getInspectionEndsAt(),
                e.getDisputeReason(), e.getReleasedAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
}

