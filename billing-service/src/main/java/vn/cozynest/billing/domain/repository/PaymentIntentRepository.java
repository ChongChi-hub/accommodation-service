package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.PaymentIntent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentIntentRepository {
    PaymentIntent save(PaymentIntent intent);
    Optional<PaymentIntent> findById(String id);
    Optional<PaymentIntent> findByPaymentCode(String paymentCode);
    Optional<PaymentIntent> findByOrderIdAndType(String orderId, String orderType);
    default List<PaymentIntent> findPendingExpiredBefore(Instant cutoff) { return List.of(); }
    default List<PaymentIntent> findAll() { return List.of(); }
}
