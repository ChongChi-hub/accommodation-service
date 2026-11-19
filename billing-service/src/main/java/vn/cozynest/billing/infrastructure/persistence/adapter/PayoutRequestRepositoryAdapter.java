package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.PayoutRequest;
import vn.cozynest.billing.domain.repository.PayoutRequestRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.PayoutRequestJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataPayoutRequestRepository;

import java.util.List;
import java.util.Optional;

@Component
public class PayoutRequestRepositoryAdapter implements PayoutRequestRepository {
    private final SpringDataPayoutRequestRepository repository;

    public PayoutRequestRepositoryAdapter(SpringDataPayoutRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public PayoutRequest save(PayoutRequest domain) {
        return toDomain(repository.save(toEntity(domain)));
    }

    @Override
    public Optional<PayoutRequest> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<PayoutRequest> findByPayoutCode(String payoutCode) {
        return repository.findByPayoutCode(payoutCode).map(this::toDomain);
    }

    @Override
    public Optional<PayoutRequest> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return Optional.empty();
        return repository.findByIdempotencyKey(idempotencyKey.trim()).map(this::toDomain);
    }

    @Override
    public List<PayoutRequest> findByLandlordId(String landlordId) {
        return repository.findByLandlordIdOrderByCreatedAtDesc(landlordId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<PayoutRequest> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private PayoutRequest toDomain(PayoutRequestJpaEntity e) {
        PayoutRequest r = new PayoutRequest(
                e.getId(), e.getPayoutCode(), e.getLandlordId(), e.getAmount(),
                e.getBankCode(), e.getAccountNumber(), e.getAccountHolder(),
                e.getStatus(), e.isOtpVerified(), e.isRequiresMakerChecker(),
                e.getMakerCheckerRequestId(), e.getFailureReason(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
        r.attachExtendedFields(e.getIdempotencyKey(), e.getBankTxCode(), e.getProofUrl(),
                e.getApprovedBy(), e.getPaidBy(), e.getPaidAt());
        return r;
    }

    private PayoutRequestJpaEntity toEntity(PayoutRequest d) {
        return PayoutRequestJpaEntity.builder()
                .id(d.getId())
                .payoutCode(d.getPayoutCode())
                .idempotencyKey(d.getIdempotencyKey())
                .landlordId(d.getLandlordId())
                .amount(d.getAmount())
                .bankCode(d.getBankCode())
                .accountNumber(d.getAccountNumber())
                .accountHolder(d.getAccountHolder())
                .status(d.getStatus())
                .otpVerified(d.isOtpVerified())
                .requiresMakerChecker(d.isRequiresMakerChecker())
                .makerCheckerRequestId(d.getMakerCheckerRequestId())
                .failureReason(d.getFailureReason())
                .bankTxCode(d.getBankTxCode())
                .proofUrl(d.getProofUrl())
                .approvedBy(d.getApprovedBy())
                .paidBy(d.getPaidBy())
                .paidAt(d.getPaidAt())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
