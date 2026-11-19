package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.PaymentProofAccessLog;
import vn.cozynest.billing.domain.model.PaymentProofRecord;

import java.util.List;
import java.util.Optional;

public interface PaymentProofRegistryRepository {
    boolean existsByNormalizedBankTxCode(String normalizedBankTxCode);
    boolean existsBySha256Hash(String sha256Hash);
    Optional<PaymentProofRecord> findById(String id);
    Optional<PaymentProofRecord> findLatestByPaymentCode(String paymentCode);
    List<PaymentProofRecord> findByOrderId(String orderId);
    List<PaymentProofRecord> findAll();
    PaymentProofRecord save(PaymentProofRecord record);
    PaymentProofAccessLog saveAccessLog(PaymentProofAccessLog accessLog);
    List<PaymentProofAccessLog> findAccessLogsByProofId(String proofId);
}

