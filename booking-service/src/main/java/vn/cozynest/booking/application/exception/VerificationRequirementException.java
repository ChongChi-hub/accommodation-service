package vn.cozynest.booking.application.exception;

public class VerificationRequirementException extends RuntimeException {
    private final String errorCode;

    public VerificationRequirementException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
