package vn.cozynest.billing.infrastructure.persistence.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.cozynest.billing.infrastructure.persistence.entity.LedgerAccountJpaEntity;

import java.util.Optional;

public interface SpringDataLedgerAccountRepository extends JpaRepository<LedgerAccountJpaEntity, String> {
    Optional<LedgerAccountJpaEntity> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM LedgerAccountJpaEntity a WHERE a.accountNumber = :accountNumber")
    Optional<LedgerAccountJpaEntity> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    Optional<LedgerAccountJpaEntity> findByOwnerIdAndAccountType(String ownerId, String accountType);
}
