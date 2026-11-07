package vn.toam.identity.domain.repository;

import vn.toam.identity.domain.model.KycEventOutbox;

import java.util.List;

public interface KycEventOutboxRepository {

    KycEventOutbox save(KycEventOutbox outbox);

    List<KycEventOutbox> findPending(int limit);
}

