package vn.cozynest.billing.application.exception;

public class ObligationAlreadyPaidException extends IllegalStateException {
    public ObligationAlreadyPaidException(String message) {
        super(message);
    }
}
