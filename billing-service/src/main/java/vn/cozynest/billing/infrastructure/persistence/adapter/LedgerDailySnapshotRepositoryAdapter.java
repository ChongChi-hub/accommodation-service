package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.LedgerDailySnapshot;
import vn.cozynest.billing.domain.repository.LedgerDailySnapshotRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.LedgerDailySnapshotJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataLedgerDailySnapshotRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class LedgerDailySnapshotRepositoryAdapter implements LedgerDailySnapshotRepository {
    private final SpringDataLedgerDailySnapshotRepository repository;

    public LedgerDailySnapshotRepositoryAdapter(SpringDataLedgerDailySnapshotRepository repository) {
        this.repository = repository;
    }

    @Override
    public LedgerDailySnapshot save(LedgerDailySnapshot snapshot) {
        LedgerDailySnapshotJpaEntity saved = repository.save(LedgerDailySnapshotJpaEntity.builder()
                .id(snapshot.getId())
                .snapshotDate(snapshot.getSnapshotDate())
                .accountId(snapshot.getAccountId())
                .accountNumber(snapshot.getAccountNumber())
                .closingBalance(snapshot.getClosingBalance())
                .createdAt(snapshot.getCreatedAt())
                .build());
        return toDomain(saved);
    }

    @Override
    public Optional<LedgerDailySnapshot> findByDateAndAccountId(LocalDate date, String accountId) {
        return repository.findBySnapshotDateAndAccountId(date, accountId).map(this::toDomain);
    }

    @Override
    public List<LedgerDailySnapshot> findByDate(LocalDate date) {
        return repository.findBySnapshotDate(date).stream().map(this::toDomain).toList();
    }

    private LedgerDailySnapshot toDomain(LedgerDailySnapshotJpaEntity e) {
        return new LedgerDailySnapshot(e.getId(), e.getSnapshotDate(), e.getAccountId(),
                e.getAccountNumber(), e.getClosingBalance(), e.getCreatedAt());
    }
}

