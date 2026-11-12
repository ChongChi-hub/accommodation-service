package vn.cozynest.billing.application.port;

import java.time.LocalDate;
import java.util.Optional;

public interface BillingContractPort {
    Optional<ContractSnapshot> getContractSnapshot(String contractId, LocalDate billingDate);

    record ContractSnapshot(
            String contractId,
            String roomId,
            String tenantId,
            String landlordId,
            String status,
            long effectiveMonthlyRent,
            int paymentDueDay
    ) {}
}

