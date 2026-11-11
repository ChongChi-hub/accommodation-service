package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.EscrowHold;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EscrowHoldRepository {
    EscrowHold save(EscrowHold hold);
    Optional<EscrowHold> findByContractId(String contractId);
    List<EscrowHold> findReleasableBefore(Instant cutoff);
}

