package vn.cozynest.booking.application.exception;

public class RentalRequestNotFoundException extends RuntimeException {
    public RentalRequestNotFoundException(String requestId) {
        super("Khong tim thay yeu cau thue: " + requestId);
    }
}

