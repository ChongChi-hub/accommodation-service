package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.billing.infrastructure.persistence.entity.PaymentProofRecordJpaEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataPaymentProofRecordRepository extends JpaRepository<PaymentProofRecordJpaEntity, String> {
    boolean existsByNormalizedBankTxCode(String normalizedBankTxCode);
    boolean existsBySha256Hash(String sha256Hash);
    Optional<PaymentProofRecordJpaEntity> findFirstByPaymentCodeOrderByCreatedAtDesc(String paymentCode);
    List<PaymentProofRecordJpaEntity> findByOrderIdOrderByCreatedAtDesc(String orderId);
}

