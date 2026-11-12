package vn.cozynest.admin.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.admin.infrastructure.persistence.entity.DeadLetterMessageJpaEntity;

import java.util.List;
import java.util.Optional;

public interface SpringDataDeadLetterRepository extends JpaRepository<DeadLetterMessageJpaEntity, String> {
    List<DeadLetterMessageJpaEntity> findAllByOrderByCreatedAtDesc();
    Optional<DeadLetterMessageJpaEntity> findByEventId(String eventId);
}

