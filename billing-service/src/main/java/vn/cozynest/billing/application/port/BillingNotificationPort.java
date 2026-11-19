package vn.cozynest.billing.application.port;

public interface BillingNotificationPort {
    void notifyDepositConfirmed(String landlordId, String contractId, long amount);

    default void notifyTenantDepositConfirmed(String tenantId, String contractId, long amount) {
    }

    default void notifyAdminDepositSubmitted(String tenantDisplayName, String contractId, String paymentCode, long amount) {
    }

    default void notifyTenantDepositRejected(String tenantId, String contractId, String paymentCode, String reason) {
    }
}
