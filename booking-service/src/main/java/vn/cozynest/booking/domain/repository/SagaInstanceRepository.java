package vn.cozynest.booking.domain.repository;

import vn.cozynest.booking.domain.model.SagaInstance;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SagaInstanceRepository {
    SagaInstance save(SagaInstance saga);
    Optional<SagaInstance> findBySagaId(String sagaId);
    List<SagaInstance> findByStatus(String status);
    List<SagaInstance> findStuckInProgressBefore(Instant cutoff);
}

