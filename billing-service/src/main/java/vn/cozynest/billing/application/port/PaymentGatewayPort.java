package vn.cozynest.billing.application.port;

/**
 * Payment gateway integration abstraction isolating vendor QR/transfer APIs from billing business logic (PAY-01).
 */
public interface PaymentGatewayPort {
    String buildPaymentInstructionUrl(String paymentCode, long amountVnd, String orderType);

    boolean isKillSwitchActive();
}

