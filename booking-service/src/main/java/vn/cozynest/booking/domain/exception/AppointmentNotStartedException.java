package vn.cozynest.booking.domain.exception;

import java.time.Instant;

public class AppointmentNotStartedException extends IllegalStateException {
    private final Instant scheduledStart;

    public AppointmentNotStartedException(Instant scheduledStart) {
        super("Khong the cap nhat ket qua truoc gio hen bat dau");
        this.scheduledStart = scheduledStart;
    }

    public Instant getScheduledStart() {
        return scheduledStart;
    }
}
