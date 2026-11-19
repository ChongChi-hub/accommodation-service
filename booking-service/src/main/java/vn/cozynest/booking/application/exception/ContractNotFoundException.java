package vn.cozynest.booking.application.exception;

public class ContractNotFoundException extends RuntimeException {
    public ContractNotFoundException(String contractId) {
        super("Khong tim thay hop dong: " + contractId);
    }
}

