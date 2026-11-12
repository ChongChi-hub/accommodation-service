package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.PaymentWebhookLog;

import java.util.List;

public interface PaymentWebhookLogRepository {
    PaymentWebhookLog save(PaymentWebhookLog log);
    List<PaymentWebhookLog> findRecent();
}

