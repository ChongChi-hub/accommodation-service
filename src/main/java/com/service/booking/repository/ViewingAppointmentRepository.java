package com.service.booking.repository;

import com.service.booking.entity.ViewingAppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ViewingAppointmentRepository extends JpaRepository<ViewingAppointmentEntity, String> {
    List<ViewingAppointmentEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<ViewingAppointmentEntity> findByRoomIdAndTenantIdAndAppointmentDateAndStatus(
            String roomId, String tenantId, LocalDate date, String status
    );
}

