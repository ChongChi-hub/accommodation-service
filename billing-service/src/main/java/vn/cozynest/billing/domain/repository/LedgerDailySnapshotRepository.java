package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.LedgerDailySnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LedgerDailySnapshotRepository {
    LedgerDailySnapshot save(LedgerDailySnapshot snapshot);
    Optional<LedgerDailySnapshot> findByDateAndAccountId(LocalDate date, String accountId);
    List<LedgerDailySnapshot> findByDate(LocalDate date);
}

