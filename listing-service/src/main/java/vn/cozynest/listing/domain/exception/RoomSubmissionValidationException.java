package vn.cozynest.listing.domain.exception;

public class RoomSubmissionValidationException extends RuntimeException {
    private final String errorCode;

    public RoomSubmissionValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
