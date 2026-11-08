package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.FaceEventOutbox;

import java.util.List;

public interface FaceEventOutboxRepository {
    FaceEventOutbox save(FaceEventOutbox outbox);
    List<FaceEventOutbox> findUnpublished(int limit);
}

