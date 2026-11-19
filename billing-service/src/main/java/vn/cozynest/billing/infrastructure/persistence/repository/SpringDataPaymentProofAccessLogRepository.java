package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.billing.infrastructure.persistence.entity.PaymentProofAccessLogJpaEntity;

import java.util.List;

@Repository
public interface SpringDataPaymentProofAccessLogRepository extends JpaRepository<PaymentProofAccessLogJpaEntity, String> {
    List<PaymentProofAccessLogJpaEntity> findByProofIdOrderByCreatedAtDesc(String proofId);
}

