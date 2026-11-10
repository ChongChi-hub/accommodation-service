package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.LedgerAccountJpaEntity;

import java.util.Optional;

public interface SpringDataLedgerAccountRepository extends JpaRepository<LedgerAccountJpaEntity, String> {
    Optional<LedgerAccountJpaEntity> findByAccountNumber(String accountNumber);
    Optional<LedgerAccountJpaEntity> findByOwnerIdAndAccountType(String ownerId, String accountType);
}

