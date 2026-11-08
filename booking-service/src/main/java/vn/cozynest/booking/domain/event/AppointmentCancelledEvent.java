package vn.cozynest.booking.domain.event;

import vn.cozynest.booking.domain.model.CancelReasonCode;
import vn.cozynest.booking.domain.model.CancelledBy;

public record AppointmentCancelledEvent(
        String appointmentId,
        String landlordId,
        String tenantId,
        String roomId,
        CancelReasonCode reasonCode,
        String reasonNote,
        CancelledBy cancelledBy
) {}
