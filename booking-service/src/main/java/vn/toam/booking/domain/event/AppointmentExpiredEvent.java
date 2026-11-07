package vn.toam.booking.domain.event;

import java.time.Instant;

public record AppointmentExpiredEvent(
        String appointmentId,
        String landlordId,
        String tenantId,
        String roomId,
        Instant scheduledStart
) {}
