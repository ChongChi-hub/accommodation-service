package vn.cozynest.billing.application.port;

import java.time.LocalDate;
import java.util.Optional;

public interface BillingContractPort {
    Optional<ContractSnapshot> getContractSnapshot(String contractId, LocalDate billingDate);

    default boolean activateContractAfterDeposit(String contractId, String paymentCode, long paidAmount) {
        return true;
    }

    record ContractSnapshot(
            String contractId,
            String roomId,
            String tenantId,
            String landlordId,
            String status,
            long effectiveMonthlyRent,
            int paymentDueDay,
            long depositAmount,
            String orderCode
    ) {
        public ContractSnapshot(
                String contractId,
                String roomId,
                String tenantId,
                String landlordId,
                String status,
                long effectiveMonthlyRent,
                int paymentDueDay
        ) {
            this(contractId, roomId, tenantId, landlordId, status, effectiveMonthlyRent, paymentDueDay, effectiveMonthlyRent, null);
        }
    }
}

