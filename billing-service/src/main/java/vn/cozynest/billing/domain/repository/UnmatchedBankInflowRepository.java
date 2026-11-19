package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.UnmatchedBankInflow;

import java.util.List;
import java.util.Optional;

public interface UnmatchedBankInflowRepository {
    boolean existsByProviderTxId(String providerTxId);
    Optional<UnmatchedBankInflow> findById(String id);
    Optional<UnmatchedBankInflow> findByProviderTxId(String providerTxId);
    List<UnmatchedBankInflow> findByStatusOrderByReceivedAtDesc(String status);
    List<UnmatchedBankInflow> findAllOrderByReceivedAtDesc();
    UnmatchedBankInflow save(UnmatchedBankInflow inflow);
}

