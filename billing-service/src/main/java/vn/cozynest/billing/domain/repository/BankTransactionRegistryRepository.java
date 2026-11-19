package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.BankTransactionRecord;

import java.util.List;
import java.util.Optional;

public interface BankTransactionRegistryRepository {
    boolean existsByNormalizedTxCode(String normalizedTxCode);
    Optional<BankTransactionRecord> findByNormalizedTxCode(String normalizedTxCode);
    BankTransactionRecord save(BankTransactionRecord record);
    List<BankTransactionRecord> findAll();
}

