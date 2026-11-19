package vn.cozynest.billing.application.port;

public interface BillingNotificationPort {
    void notifyDepositConfirmed(String landlordId, String contractId, long amount);
}

