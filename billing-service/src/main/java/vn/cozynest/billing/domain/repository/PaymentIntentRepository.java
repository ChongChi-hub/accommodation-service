package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.PaymentIntent;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentIntentRepository {
    PaymentIntent save(PaymentIntent intent);
    Optional<PaymentIntent> findById(String id);
    Optional<PaymentIntent> findByPaymentCode(String paymentCode);
    Optional<PaymentIntent> findByOrderIdAndType(String orderId, String orderType);
    default List<PaymentIntent> findByOrderIdAndOrderType(String orderId, String orderType) { return List.of(); }
    default List<PaymentIntent> findPendingExpiredBefore(Instant cutoff) { return List.of(); }
    default List<PaymentIntent> findByOrderIdAndOrderTypeAndStatus(String orderId, String orderType, String status) { return List.of(); }
    default List<PaymentIntent> findByOrderIdAndOrderTypeAndStatusIn(String orderId, String orderType, Collection<String> statuses) { return List.of(); }
    default List<PaymentIntent> findByStatus(String status) { return findAll().stream().filter(i -> status.equals(i.getStatus())).toList(); }
    default List<PaymentIntent> findAll() { return List.of(); }
}
