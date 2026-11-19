package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.PaymentIntentJpaEntity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataPaymentIntentRepository extends JpaRepository<PaymentIntentJpaEntity, String> {
    Optional<PaymentIntentJpaEntity> findByPaymentCode(String paymentCode);
    List<PaymentIntentJpaEntity> findByOrderIdAndOrderType(String orderId, String orderType);
    List<PaymentIntentJpaEntity> findByStatusAndExpiresAtBefore(String status, Instant cutoff);
    List<PaymentIntentJpaEntity> findByOrderIdAndOrderTypeAndStatus(String orderId, String orderType, String status);
    List<PaymentIntentJpaEntity> findByOrderIdAndOrderTypeAndStatusIn(String orderId, String orderType, Collection<String> statuses);
    List<PaymentIntentJpaEntity> findByStatusInAndExpiresAtBefore(Collection<String> statuses, Instant cutoff);
}
