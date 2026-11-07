package vn.toam.booking.application.dto;

import java.time.Instant;

public record AppointmentStatusHistoryDto(
        Long id,
        String appointmentId,
        String fromStatus,
        String toStatus,
        String actorId,
        String actorRole,
        String reason,
        Instant createdAt
) {}
