package vn.cozynest.booking.application.dto;

public record ConfirmAppointmentRequest(
        Boolean force,
        String landlordNote,
        Long version
) {}
