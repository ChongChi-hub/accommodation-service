package vn.cozynest.booking.infrastructure.billing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.cozynest.booking.application.port.BookingBillingPort;

@Component
public class HttpBookingBillingAdapter implements BookingBillingPort {
    private static final Logger log = LoggerFactory.getLogger(HttpBookingBillingAdapter.class);

    @Override
    public void openEscrowHold(String contractId, String tenantId, String landlordId, long amount) {
        log.info("Escrow hold registration requested for contractId={}, amount={}", contractId, amount);
    }

    @Override
    public void refundOrReleaseOnCompensation(String contractId, String reason) {
        log.warn("Saga compensation signaled billing escrow release/refund for contractId={}, reason={}", contractId, reason);
    }
}

