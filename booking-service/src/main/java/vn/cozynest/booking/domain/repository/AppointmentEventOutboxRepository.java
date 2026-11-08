package vn.cozynest.booking.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.booking.domain.model.AppointmentEventOutbox;

import java.util.List;

@Repository
public interface AppointmentEventOutboxRepository extends JpaRepository<AppointmentEventOutbox, Long> {

    List<AppointmentEventOutbox> findByPublishedAtIsNullOrderByCreatedAtAsc();
}
