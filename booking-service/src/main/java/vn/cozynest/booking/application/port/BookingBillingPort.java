package vn.cozynest.booking.application.port;

/**
 * Port to billing-service for deposit escrow hold coordination & saga compensation.
 */
public interface BookingBillingPort {
    void openEscrowHold(String contractId, String tenantId, String landlordId, long amount);

    void refundOrReleaseOnCompensation(String contractId, String reason);
}

