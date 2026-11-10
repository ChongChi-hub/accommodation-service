package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.util.UUID;

public class LedgerAccount {
    private final String id;
    private final String accountNumber;
    private final String accountName;
    private final AccountType accountType;
    private final String ownerId;
    private long balance;
    private final String currency;
    private boolean frozen;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public LedgerAccount(String id, String accountNumber, String accountName, AccountType accountType,
                         String ownerId, long balance, String currency, boolean frozen, long version,
                         Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.accountType = accountType;
        this.ownerId = ownerId;
        this.balance = balance;
        this.currency = currency != null ? currency : "VND";
        this.frozen = frozen;
        this.version = version;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static LedgerAccount create(String accountNumber, String accountName, AccountType accountType, String ownerId) {
        return new LedgerAccount(
                UUID.randomUUID().toString(),
                accountNumber,
                accountName,
                accountType,
                ownerId,
                0L,
                "VND",
                false,
                0L,
                Instant.now(),
                Instant.now()
        );
    }

    public void applyPosting(PostingDirection direction, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien ghi so phai lon hon 0");
        }
        if (frozen) {
            throw new IllegalStateException("Tai khoan ledger " + accountNumber + " dang bi dong bang");
        }

        // ASSET & EXPENSE: Debit increases, Credit decreases
        // LIABILITY, EQUITY, REVENUE: Credit increases, Debit decreases
        boolean isNormalDebit = (accountType == AccountType.ASSET || accountType == AccountType.EXPENSE);
        if (isNormalDebit) {
            if (direction == PostingDirection.DEBIT) {
                this.balance += amount;
            } else {
                this.balance -= amount;
            }
        } else {
            if (direction == PostingDirection.CREDIT) {
                this.balance += amount;
            } else {
                this.balance -= amount;
            }
        }
        this.updatedAt = Instant.now();
    }

    public void freeze() {
        this.frozen = true;
        this.updatedAt = Instant.now();
    }

    public void unfreeze() {
        this.frozen = false;
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountName() { return accountName; }
    public AccountType getAccountType() { return accountType; }
    public String getOwnerId() { return ownerId; }
    public long getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public boolean isFrozen() { return frozen; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

