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
                .sandboxMode(h.isSandboxMode())
                .tenantConfirmedMoveIn(h.isTenantConfirmedMoveIn())
                .landlordConfirmedMoveIn(h.isLandlordConfirmedMoveIn())
                .moveInConfirmedAt(h.getMoveInConfirmedAt())
                .inspectionEndsAt(h.getInspectionEndsAt())
                .disputeReason(h.getDisputeReason())
                .paymentCode(h.getPaymentCode())
                .bankTxCode(h.getBankTxCode())
                .receiptHash(h.getReceiptHash())
                .receiptUrl(h.getReceiptUrl())
                .releasedAt(h.getReleasedAt())
                .createdAt(h.getCreatedAt()).updatedAt(h.getUpdatedAt())
                .build());
        return toDomain(saved);
    }

    @Override
    public boolean existsByBankTxCode(String bankTxCode) {
        return bankTxCode != null && !bankTxCode.isBlank() && repository.existsByBankTxCode(bankTxCode);
    }

    @Override
    public boolean existsByReceiptHash(String receiptHash) {
        return receiptHash != null && !receiptHash.isBlank() && repository.existsByReceiptHash(receiptHash);
    }

    @Override
    public Optional<EscrowHold> findByContractId(String contractId) {
        return repository.findByContractId(contractId).map(this::toDomain);
    }

    @Override
    public List<EscrowHold> findReleasableBefore(Instant cutoff) {
        return repository.findByStatusAndInspectionEndsAtBefore("HELD", cutoff).stream().map(this::toDomain).toList();
    }

    @Override
    public List<EscrowHold> findByTenantId(String tenantId) {
        return repository.findByTenantId(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<EscrowHold> findByLandlordId(String landlordId) {
        return repository.findByLandlordId(landlordId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<EscrowHold> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private EscrowHold toDomain(EscrowHoldJpaEntity e) {
        EscrowHold hold = new EscrowHold(e.getId(), e.getContractId(), e.getTenantId(), e.getLandlordId(),
                e.getAmount(), e.getStatus(), e.isSandboxMode(),
                e.isTenantConfirmedMoveIn(), e.isLandlordConfirmedMoveIn(), e.getMoveInConfirmedAt(),
                e.getInspectionEndsAt(),
                e.getDisputeReason(), e.getReleasedAt(), e.getCreatedAt(), e.getUpdatedAt());
        hold.attachPaymentProof(e.getPaymentCode(), e.getBankTxCode(), e.getReceiptHash(), e.getReceiptUrl());
        return hold;
    }
}

