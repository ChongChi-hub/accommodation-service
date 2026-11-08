package vn.cozynest.booking.application.exception;

public class AppointmentNotFoundException extends RuntimeException {
    public AppointmentNotFoundException(String id) {
        super("Khong tim thay lich hen voi ma: " + id);
    }
}
