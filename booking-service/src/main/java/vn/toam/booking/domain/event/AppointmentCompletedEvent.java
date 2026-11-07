package vn.toam.booking.domain.event;

import java.time.Instant;

public record AppointmentCompletedEvent(
        String appointmentId,
        String landlordId,
        String tenantId,
        String roomId,
        String note,
        Instant completedAt
) {}
