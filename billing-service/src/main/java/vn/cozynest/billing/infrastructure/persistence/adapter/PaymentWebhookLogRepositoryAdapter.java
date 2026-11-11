package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.PaymentWebhookLog;
import vn.cozynest.billing.domain.repository.PaymentWebhookLogRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.PaymentWebhookLogJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataPaymentWebhookLogRepository;

import java.util.List;

@Component
public class PaymentWebhookLogRepositoryAdapter implements PaymentWebhookLogRepository {
    private final SpringDataPaymentWebhookLogRepository repository;

    public PaymentWebhookLogRepositoryAdapter(SpringDataPaymentWebhookLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentWebhookLog save(PaymentWebhookLog log) {
        PaymentWebhookLogJpaEntity saved = repository.save(PaymentWebhookLogJpaEntity.builder()
                .id(log.getId())
                .paymentCode(log.getPaymentCode())
                .sourceIp(log.getSourceIp())
                .signatureValid(log.isSignatureValid())
                .processingStatus(log.getProcessingStatus())
                .errorMessage(log.getErrorMessage())
                .rawPayload(log.getRawPayload())
                .receivedAt(log.getReceivedAt())
                .build());
        return toDomain(saved);
    }

    @Override
    public List<PaymentWebhookLog> findRecent() {
        return repository.findTop50ByOrderByReceivedAtDesc().stream().map(this::toDomain).toList();
    }

    private PaymentWebhookLog toDomain(PaymentWebhookLogJpaEntity e) {
        return new PaymentWebhookLog(e.getId(), e.getPaymentCode(), e.getSourceIp(), e.isSignatureValid(),
                e.getProcessingStatus(), e.getErrorMessage(), e.getRawPayload(), e.getReceivedAt());
    }
}

