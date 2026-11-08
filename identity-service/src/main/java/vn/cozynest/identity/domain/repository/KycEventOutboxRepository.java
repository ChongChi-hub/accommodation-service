package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.KycEventOutbox;

import java.util.List;

public interface KycEventOutboxRepository {

    KycEventOutbox save(KycEventOutbox outbox);

    List<KycEventOutbox> findPending(int limit);
}

