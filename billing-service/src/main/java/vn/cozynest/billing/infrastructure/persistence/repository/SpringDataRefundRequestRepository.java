package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.RefundRequestJpaEntity;

import java.util.List;

public interface SpringDataRefundRequestRepository extends JpaRepository<RefundRequestJpaEntity, String> {
    List<RefundRequestJpaEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<RefundRequestJpaEntity> findByPaymentIntentId(String paymentIntentId);
    List<RefundRequestJpaEntity> findByStatusOrderByUpdatedAtDesc(String status);
}

