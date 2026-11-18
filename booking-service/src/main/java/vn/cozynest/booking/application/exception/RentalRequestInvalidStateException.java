package vn.cozynest.booking.application.exception;

public class RentalRequestInvalidStateException extends RuntimeException {
    private final String currentStatus;

    public RentalRequestInvalidStateException(String currentStatus, String message) {
        super(message);
        this.currentStatus = currentStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }
}

