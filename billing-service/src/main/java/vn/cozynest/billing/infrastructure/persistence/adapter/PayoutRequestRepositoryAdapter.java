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
        PayoutRequestJpaEntity entity = toEntity(domain);
        PayoutRequestJpaEntity saved = repository.save(entity);
        return toDomain(saved);
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
    public List<PayoutRequest> findByLandlordId(String landlordId) {
        return repository.findByLandlordIdOrderByCreatedAtDesc(landlordId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<PayoutRequest> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private PayoutRequest toDomain(PayoutRequestJpaEntity entity) {
        return new PayoutRequest(
                entity.getId(),
                entity.getPayoutCode(),
                entity.getLandlordId(),
                entity.getAmount(),
                entity.getBankCode(),
                entity.getAccountNumber(),
                entity.getAccountHolder(),
                entity.getStatus(),
                entity.isOtpVerified(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PayoutRequestJpaEntity toEntity(PayoutRequest domain) {
        return PayoutRequestJpaEntity.builder()
                .id(domain.getId())
                .payoutCode(domain.getPayoutCode())
                .landlordId(domain.getLandlordId())
                .amount(domain.getAmount())
                .bankCode(domain.getBankCode())
                .accountNumber(domain.getAccountNumber())
                .accountHolder(domain.getAccountHolder())
                .status(domain.getStatus())
                .otpVerified(domain.isOtpVerified())
                .failureReason(domain.getFailureReason())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

