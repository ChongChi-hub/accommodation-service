package vn.cozynest.booking.domain.repository;

import vn.cozynest.booking.domain.model.AppointmentEventOutbox;

import java.util.List;
import java.util.Optional;

public interface AppointmentEventOutboxRepository {

    AppointmentEventOutbox save(AppointmentEventOutbox outbox);

    List<AppointmentEventOutbox> findUnprocessed();

    Optional<AppointmentEventOutbox> findById(String id);
}
