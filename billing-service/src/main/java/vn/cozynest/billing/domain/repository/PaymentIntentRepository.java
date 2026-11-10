package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.PaymentIntent;

import java.util.Optional;

public interface PaymentIntentRepository {
    PaymentIntent save(PaymentIntent intent);
    Optional<PaymentIntent> findById(String id);
    Optional<PaymentIntent> findByPaymentCode(String paymentCode);
    Optional<PaymentIntent> findByOrderIdAndType(String orderId, String orderType);
}

