package vn.toam.booking.domain.event;

import java.time.Instant;

public record AppointmentConfirmedEvent(
        String appointmentId,
        String landlordId,
        String tenantId,
        String roomId,
        Instant scheduledStart,
        Instant scheduledEnd,
        boolean forced,
        String landlordNote
) {}
