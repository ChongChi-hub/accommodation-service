package vn.cozynest.booking.domain.repository;

import vn.cozynest.booking.domain.model.AppointmentStatusHistory;

import java.util.List;

public interface AppointmentStatusHistoryRepository {

    AppointmentStatusHistory save(AppointmentStatusHistory history);

    List<AppointmentStatusHistory> findByAppointmentIdOrderByCreatedAtAsc(String appointmentId);
}
