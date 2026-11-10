package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.util.UUID;

public class LedgerPosting {
    private final String id;
    private final String journalEntryId;
    private final String accountId;
    private final PostingDirection direction;
    private final long amount;
    private final Instant createdAt;

    public LedgerPosting(String id, String journalEntryId, String accountId,
                         PostingDirection direction, long amount, Instant createdAt) {
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien dinh khoan phai lon hon 0");
        }
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.journalEntryId = journalEntryId;
        this.accountId = accountId;
        this.direction = direction;
        this.amount = amount;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static LedgerPosting create(String journalEntryId, String accountId,
                                       PostingDirection direction, long amount) {
        return new LedgerPosting(UUID.randomUUID().toString(), journalEntryId, accountId, direction, amount, Instant.now());
    }

    public String getId() { return id; }
    public String getJournalEntryId() { return journalEntryId; }
    public String getAccountId() { return accountId; }
    public PostingDirection getDirection() { return direction; }
    public long getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}

