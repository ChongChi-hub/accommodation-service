package vn.cozynest.booking.application.dto;

import vn.cozynest.booking.domain.model.CancelReasonCode;

public record CancelAppointmentRequest(
        CancelReasonCode reasonCode,
        String reasonNote,
        Long version
) {}
