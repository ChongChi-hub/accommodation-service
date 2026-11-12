package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "ledger_daily_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerDailySnapshotJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "closing_balance", nullable = false)
    private long closingBalance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

