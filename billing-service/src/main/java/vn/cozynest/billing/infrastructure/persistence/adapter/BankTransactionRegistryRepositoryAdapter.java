package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.BankTransactionRecord;
import vn.cozynest.billing.domain.model.PaymentReferenceCodeCodec;
import vn.cozynest.billing.domain.repository.BankTransactionRegistryRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.BankTransactionRecordJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataBankTransactionRegistryRepository;

import java.util.List;
import java.util.Optional;

@Component
public class BankTransactionRegistryRepositoryAdapter implements BankTransactionRegistryRepository {
    private final SpringDataBankTransactionRegistryRepository jpaRepository;

    public BankTransactionRegistryRepositoryAdapter(SpringDataBankTransactionRegistryRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsByNormalizedTxCode(String normalizedTxCode) {
        String norm = PaymentReferenceCodeCodec.normalizeAlphanumericUpper(normalizedTxCode);
        return !norm.isEmpty() && jpaRepository.existsByNormalizedTxCode(norm);
    }

    @Override
    public Optional<BankTransactionRecord> findByNormalizedTxCode(String normalizedTxCode) {
        String norm = PaymentReferenceCodeCodec.normalizeAlphanumericUpper(normalizedTxCode);
        return norm.isEmpty() ? Optional.empty() : jpaRepository.findByNormalizedTxCode(norm).map(this::toDomain);
    }

    @Override
    public BankTransactionRecord save(BankTransactionRecord d) {
        BankTransactionRecordJpaEntity e = BankTransactionRecordJpaEntity.builder()
                .id(d.getId())
                .normalizedTxCode(d.getNormalizedTxCode())
                .provider(d.getProvider())
                .direction(d.getDirection())
                .amount(d.getAmount())
                .referenceCode(d.getReferenceCode())
                .orderType(d.getOrderType())
                .orderId(d.getOrderId())
                .receiverAccountMasked(d.getReceiverAccountMasked())
                .rawContent(d.getRawContent())
                .sanitizedPayload(d.getSanitizedPayload())
                .recordedBy(d.getRecordedBy())
                .occurredAt(d.getOccurredAt())
                .createdAt(d.getCreatedAt())
                .build();
        return toDomain(jpaRepository.save(e));
    }

    @Override
    public List<BankTransactionRecord> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private BankTransactionRecord toDomain(BankTransactionRecordJpaEntity e) {
        return new BankTransactionRecord(
                e.getId(), e.getNormalizedTxCode(), e.getProvider(), e.getDirection(),
                e.getAmount(), e.getReferenceCode(), e.getOrderType(), e.getOrderId(),
                e.getReceiverAccountMasked(), e.getRawContent(), e.getSanitizedPayload(),
                e.getRecordedBy(), e.getOccurredAt(), e.getCreatedAt()
        );
    }
}

