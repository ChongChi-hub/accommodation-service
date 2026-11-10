package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.PayoutRequest;

import java.util.List;
import java.util.Optional;

public interface PayoutRequestRepository {
    PayoutRequest save(PayoutRequest request);
    Optional<PayoutRequest> findById(String id);
    Optional<PayoutRequest> findByPayoutCode(String payoutCode);
    List<PayoutRequest> findByLandlordId(String landlordId);
    List<PayoutRequest> findAll();
}

