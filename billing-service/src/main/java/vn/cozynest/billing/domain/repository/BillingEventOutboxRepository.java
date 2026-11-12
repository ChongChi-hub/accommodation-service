package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.BillingEventOutbox;

import java.util.List;

public interface BillingEventOutboxRepository {
    BillingEventOutbox save(BillingEventOutbox event);
    List<BillingEventOutbox> findPendingEvents();
}

