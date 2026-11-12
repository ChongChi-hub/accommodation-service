package vn.cozynest.communication.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface SpringDataEventInboxRepository extends JpaRepository<EventInboxJpaEntity, String> {

    @Query("""
            SELECT MAX(e.occurredAt) FROM EventInboxJpaEntity e
            WHERE e.aggregateType = :aggregateType AND e.aggregateId = :aggregateId AND e.eventType = :eventType
            """)
    Optional<Instant> findMaxOccurredAt(
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") String aggregateId,
            @Param("eventType") String eventType
    );
}

