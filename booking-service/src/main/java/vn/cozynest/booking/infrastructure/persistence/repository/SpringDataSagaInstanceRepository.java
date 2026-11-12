package vn.cozynest.booking.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.cozynest.booking.infrastructure.persistence.entity.SagaInstanceJpaEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataSagaInstanceRepository extends JpaRepository<SagaInstanceJpaEntity, String> {
    Optional<SagaInstanceJpaEntity> findBySagaId(String sagaId);
    List<SagaInstanceJpaEntity> findByStatus(String status);

    @Query("SELECT s FROM SagaInstanceJpaEntity s WHERE s.status = 'IN_PROGRESS' AND s.updatedAt < :cutoff")
    List<SagaInstanceJpaEntity> findStuckInProgressBefore(@Param("cutoff") Instant cutoff);
}

