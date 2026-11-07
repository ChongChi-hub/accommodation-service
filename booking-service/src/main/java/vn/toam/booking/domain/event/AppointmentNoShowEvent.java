package vn.toam.booking.domain.event;

public record AppointmentNoShowEvent(
        String appointmentId,
        String landlordId,
        String tenantId,
        String roomId,
        String note
) {}
