package vn.cozynest.billing.application.exception;

/**
 * Security exception distinguishing 401 invalid signature from 403 forbidden source IP/ownership.
 */
public class WebhookSecurityException extends SecurityException {
    private final int httpStatus;

    public WebhookSecurityException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}

