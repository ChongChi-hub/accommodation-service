package vn.toam.booking.application.dto;

import vn.toam.booking.domain.model.CancelReasonCode;

public record CancelAppointmentRequest(
        CancelReasonCode reasonCode,
        String reasonNote,
        Long version
) {}
