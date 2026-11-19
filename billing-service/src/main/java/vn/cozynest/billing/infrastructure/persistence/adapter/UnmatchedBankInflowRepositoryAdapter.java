package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.PaymentReferenceCodeCodec;
import vn.cozynest.billing.domain.model.UnmatchedBankInflow;
import vn.cozynest.billing.domain.repository.UnmatchedBankInflowRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.UnmatchedBankInflowJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataUnmatchedBankInflowRepository;

import java.util.List;
import java.util.Optional;

@Component
public class UnmatchedBankInflowRepositoryAdapter implements UnmatchedBankInflowRepository {
    private final SpringDataUnmatchedBankInflowRepository jpaRepository;

    public UnmatchedBankInflowRepositoryAdapter(SpringDataUnmatchedBankInflowRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsByProviderTxId(String providerTxId) {
        String norm = PaymentReferenceCodeCodec.normalizeAlphanumericUpper(providerTxId);
        return !norm.isEmpty() && jpaRepository.existsByProviderTxId(norm);
    }

    @Override
    public Optional<UnmatchedBankInflow> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<UnmatchedBankInflow> findByProviderTxId(String providerTxId) {
        String norm = PaymentReferenceCodeCodec.normalizeAlphanumericUpper(providerTxId);
        return norm.isEmpty() ? Optional.empty() : jpaRepository.findByProviderTxId(norm).map(this::toDomain);
    }

    @Override
    public List<UnmatchedBankInflow> findByStatusOrderByReceivedAtDesc(String status) {
        return jpaRepository.findByStatusOrderByReceivedAtDesc(status).stream().map(this::toDomain).toList();
    }

    @Override
    public List<UnmatchedBankInflow> findAllOrderByReceivedAtDesc() {
        return jpaRepository.findAllByOrderByReceivedAtDesc().stream().map(this::toDomain).toList();
    }

    @Override
    public UnmatchedBankInflow save(UnmatchedBankInflow d) {
        UnmatchedBankInflowJpaEntity e = UnmatchedBankInflowJpaEntity.builder()
                .id(d.getId())
                .provider(d.getProvider())
                .providerTxId(d.getProviderTxId())
                .amount(d.getAmount())
                .rawContent(d.getRawContent())
                .extractedReference(d.getExtractedReference())
                .mismatchReason(d.getMismatchReason())
                .matchedPaymentCode(d.getMatchedPaymentCode())
                .receiverAccountMasked(d.getReceiverAccountMasked())
                .sanitizedPayload(d.getSanitizedPayload())
                .status(d.getStatus())
                .resolvedBy(d.getResolvedBy())
                .resolutionNote(d.getResolutionNote())
                .resolvedAt(d.getResolvedAt())
                .receivedAt(d.getReceivedAt())
                .build();
        return toDomain(jpaRepository.save(e));
    }

    private UnmatchedBankInflow toDomain(UnmatchedBankInflowJpaEntity e) {
        return new UnmatchedBankInflow(
                e.getId(), e.getProvider(), e.getProviderTxId(), e.getAmount(),
                e.getRawContent(), e.getExtractedReference(), e.getMismatchReason(),
                e.getMatchedPaymentCode(), e.getReceiverAccountMasked(), e.getSanitizedPayload(),
                e.getStatus(), e.getResolvedBy(), e.getResolutionNote(),
                e.getResolvedAt(), e.getReceivedAt()
        );
    }
}

