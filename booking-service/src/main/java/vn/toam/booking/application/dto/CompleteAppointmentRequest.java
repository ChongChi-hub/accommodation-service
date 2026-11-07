package vn.toam.booking.application.dto;

import vn.toam.booking.domain.model.AppointmentStatus;

public record CompleteAppointmentRequest(
        AppointmentStatus outcome,
        String note,
        Long version
) {}
