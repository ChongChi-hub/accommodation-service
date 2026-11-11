package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.LedgerDailySnapshotJpaEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataLedgerDailySnapshotRepository extends JpaRepository<LedgerDailySnapshotJpaEntity, String> {
    Optional<LedgerDailySnapshotJpaEntity> findBySnapshotDateAndAccountId(LocalDate date, String accountId);
    List<LedgerDailySnapshotJpaEntity> findBySnapshotDate(LocalDate date);
}

