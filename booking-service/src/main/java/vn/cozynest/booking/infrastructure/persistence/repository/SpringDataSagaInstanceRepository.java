package vn.cozynest.booking.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.booking.infrastructure.persistence.entity.SagaInstanceJpaEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataSagaInstanceRepository extends JpaRepository<SagaInstanceJpaEntity, String> {
    Optional<SagaInstanceJpaEntity> findBySagaId(String sagaId);
    List<SagaInstanceJpaEntity> findByStatus(String status);
}

