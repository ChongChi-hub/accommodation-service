package vn.cozynest.booking.application.dto;

import vn.cozynest.booking.domain.model.AppointmentStatus;

public record CompleteAppointmentRequest(
        AppointmentStatus outcome,
        String note,
        Long version
) {}
