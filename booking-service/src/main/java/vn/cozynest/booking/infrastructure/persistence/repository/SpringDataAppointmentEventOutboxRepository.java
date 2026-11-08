package vn.cozynest.booking.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.booking.infrastructure.persistence.entity.AppointmentEventOutboxJpaEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataAppointmentEventOutboxRepository
        extends JpaRepository<AppointmentEventOutboxJpaEntity, Long> {

    List<AppointmentEventOutboxJpaEntity> findByPublishedAtIsNullOrderByCreatedAtAsc();

    Optional<AppointmentEventOutboxJpaEntity> findByEventId(String eventId);
}
