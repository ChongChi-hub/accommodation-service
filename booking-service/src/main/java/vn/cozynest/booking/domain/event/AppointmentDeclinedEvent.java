package vn.cozynest.booking.domain.event;

import vn.cozynest.booking.domain.model.CancelReasonCode;

public record AppointmentDeclinedEvent(
        String appointmentId,
        String landlordId,
        String tenantId,
        String roomId,
        CancelReasonCode reasonCode,
        String reasonNote
) {}
