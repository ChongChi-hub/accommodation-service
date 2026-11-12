package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.RefundRequest;

import java.util.List;
import java.util.Optional;

public interface RefundRequestRepository {
    RefundRequest save(RefundRequest request);
    Optional<RefundRequest> findById(String id);
    List<RefundRequest> findByTenantId(String tenantId);
    List<RefundRequest> findAll();
    default List<RefundRequest> findByPaymentIntentId(String paymentIntentId) { return List.of(); }
    default List<RefundRequest> findByStatus(String status) { return List.of(); }
}

