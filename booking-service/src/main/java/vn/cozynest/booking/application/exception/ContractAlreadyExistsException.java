package vn.cozynest.booking.application.exception;

public class ContractAlreadyExistsException extends RuntimeException {
    public ContractAlreadyExistsException(String requestId) {
        super("Yeu cau thue da co hop dong duoc tao: " + requestId);
    }
}

