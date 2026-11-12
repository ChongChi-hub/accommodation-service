package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class LedgerDailySnapshot {
    private final String id;
    private final LocalDate snapshotDate;
    private final String accountId;
    private final String accountNumber;
    private final long closingBalance;
    private final Instant createdAt;

    public LedgerDailySnapshot(String id, LocalDate snapshotDate, String accountId,
                               String accountNumber, long closingBalance, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.snapshotDate = snapshotDate;
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.closingBalance = closingBalance;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public String getId() { return id; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public String getAccountId() { return accountId; }
    public String getAccountNumber() { return accountNumber; }
    public long getClosingBalance() { return closingBalance; }
    public Instant getCreatedAt() { return createdAt; }
}

