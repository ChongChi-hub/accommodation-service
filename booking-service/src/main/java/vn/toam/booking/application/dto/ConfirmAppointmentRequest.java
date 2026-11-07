package vn.toam.booking.application.dto;

public record ConfirmAppointmentRequest(
        Boolean force,
        String landlordNote,
        Long version
) {}
