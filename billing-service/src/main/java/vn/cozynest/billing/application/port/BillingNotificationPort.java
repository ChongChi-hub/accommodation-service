package vn.cozynest.billing.application.port;

public interface BillingNotificationPort {
    void notifyDepositConfirmed(String landlordId, String contractId, long amount);

    default void notifyTenantDepositConfirmed(String tenantId, String contractId, long amount) {}

    default void notifyAdminDepositSubmitted(String tenantDisplayName, String contractId, String paymentCode, long amount) {
        notifyAdminPaymentSubmitted(tenantDisplayName, "DEPOSIT", contractId, paymentCode, amount);
    }

    default void notifyAdminPaymentSubmitted(String tenantDisplayName, String orderType, String orderId, String paymentCode, long amount) {}

    default void notifyTenantDepositRejected(String tenantId, String contractId, String paymentCode, String reason) {}

    default void notifyTenantInvoiceCreated(String tenantId, String invoiceCode, String periodMonth, long totalAmount) {}

    default void notifyInvoicePaidWithCommission(String landlordId, String tenantId, String invoiceCode, long totalAmount, long commissionFee, long landlordNet) {}

    default void notifyAdminPayoutRequested(String landlordId, String payoutCode, long amount, String bankCode, String maskedAcc) {}

    default void notifyLandlordPayoutPaid(String landlordId, String payoutCode, long amount, String bankTxCode) {}
}
