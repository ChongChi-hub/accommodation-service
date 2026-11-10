package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.AccountType;
import vn.cozynest.billing.domain.model.LedgerAccount;

import java.util.List;
import java.util.Optional;

public interface LedgerAccountRepository {
    LedgerAccount save(LedgerAccount account);
    Optional<LedgerAccount> findById(String id);
    Optional<LedgerAccount> findByAccountNumber(String accountNumber);
    Optional<LedgerAccount> findByOwnerIdAndType(String ownerId, AccountType type);
    List<LedgerAccount> findAll();
}

