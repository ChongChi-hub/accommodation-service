package vn.toam.identity.domain.exception;

public class IdentityDomainException extends RuntimeException {
    private final String errorCode;

    public IdentityDomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

