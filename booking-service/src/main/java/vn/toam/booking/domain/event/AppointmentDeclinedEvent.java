package vn.toam.booking.domain.event;

import vn.toam.booking.domain.model.CancelReasonCode;

public record AppointmentDeclinedEvent(
        String appointmentId,
        String landlordId,
        String tenantId,
        String roomId,
        CancelReasonCode reasonCode,
        String reasonNote
) {}
