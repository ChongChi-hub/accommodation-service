package vn.cozynest.listing.domain.exception;

public class RateLimitExceededException extends RuntimeException {
    private final String errorCode;

    public RateLimitExceededException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
