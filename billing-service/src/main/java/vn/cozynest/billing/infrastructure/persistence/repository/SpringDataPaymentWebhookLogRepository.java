package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.PaymentWebhookLogJpaEntity;

import java.util.List;

public interface SpringDataPaymentWebhookLogRepository extends JpaRepository<PaymentWebhookLogJpaEntity, String> {
    List<PaymentWebhookLogJpaEntity> findTop50ByOrderByReceivedAtDesc();
}

