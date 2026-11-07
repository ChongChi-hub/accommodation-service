package vn.toam.booking.domain.event;

import vn.toam.booking.domain.model.CancelReasonCode;
import vn.toam.booking.domain.model.CancelledBy;

public record AppointmentCancelledEvent(
        String appointmentId,
        String landlordId,
        String tenantId,
        String roomId,
        CancelReasonCode reasonCode,
        String reasonNote,
        CancelledBy cancelledBy
) {}
