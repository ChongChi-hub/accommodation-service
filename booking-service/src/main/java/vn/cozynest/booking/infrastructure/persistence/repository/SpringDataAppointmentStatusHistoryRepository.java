package vn.cozynest.booking.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.booking.infrastructure.persistence.entity.AppointmentStatusHistoryJpaEntity;

import java.util.List;

@Repository
public interface SpringDataAppointmentStatusHistoryRepository
        extends JpaRepository<AppointmentStatusHistoryJpaEntity, Long> {

    List<AppointmentStatusHistoryJpaEntity> findByAppointmentIdOrderByCreatedAtAsc(String appointmentId);
}
