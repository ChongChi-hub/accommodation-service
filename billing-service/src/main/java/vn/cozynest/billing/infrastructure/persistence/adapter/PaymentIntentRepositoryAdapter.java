package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.PaymentIntent;
import vn.cozynest.billing.domain.repository.PaymentIntentRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.PaymentIntentJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataPaymentIntentRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class PaymentIntentRepositoryAdapter implements PaymentIntentRepository {
    private final SpringDataPaymentIntentRepository repository;

    public PaymentIntentRepositoryAdapter(SpringDataPaymentIntentRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentIntent save(PaymentIntent domain) {
        PaymentIntentJpaEntity entity = toEntity(domain);
        PaymentIntentJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<PaymentIntent> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<PaymentIntent> findByPaymentCode(String paymentCode) {
        return repository.findByPaymentCode(paymentCode).map(this::toDomain);
    }

    @Override
    public Optional<PaymentIntent> findByOrderIdAndType(String orderId, String orderType) {
        return repository.findByOrderIdAndOrderType(orderId, orderType).stream().findFirst().map(this::toDomain);
    }

    @Override
    public List<PaymentIntent> findByOrderIdAndOrderType(String orderId, String orderType) {
        return repository.findByOrderIdAndOrderType(orderId, orderType).stream().map(this::toDomain).toList();
    }

    @Override
    public List<PaymentIntent> findPendingExpiredBefore(Instant cutoff) {
        return repository.findByStatusInAndExpiresAtBefore(List.of("PENDING", "SUBMITTED"), cutoff)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<PaymentIntent> findByOrderIdAndOrderTypeAndStatus(String orderId, String orderType, String status) {
        return repository.findByOrderIdAndOrderTypeAndStatus(orderId, orderType, status).stream().map(this::toDomain).toList();
    }

    @Override
    public List<PaymentIntent> findByOrderIdAndOrderTypeAndStatusIn(String orderId, String orderType, Collection<String> statuses) {
        return repository.findByOrderIdAndOrderTypeAndStatusIn(orderId, orderType, statuses).stream().map(this::toDomain).toList();
    }

    @Override
    public List<PaymentIntent> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private PaymentIntent toDomain(PaymentIntentJpaEntity entity) {
        return new PaymentIntent(
                entity.getId(),
                entity.getPaymentCode(),
                entity.getPayerId(),
                entity.getOrderType(),
                entity.getOrderId(),
                entity.getAmount(),
                entity.getPaidAmount(),
                entity.getQrUrl(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PaymentIntentJpaEntity toEntity(PaymentIntent domain) {
        return PaymentIntentJpaEntity.builder()
                .id(domain.getId())
                .paymentCode(domain.getPaymentCode())
                .payerId(domain.getPayerId())
                .orderType(domain.getOrderType())
                .orderId(domain.getOrderId())
                .amount(domain.getAmount())
                .paidAmount(domain.getPaidAmount())
                .qrUrl(domain.getQrUrl())
                .status(domain.getStatus())
                .expiresAt(domain.getExpiresAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
