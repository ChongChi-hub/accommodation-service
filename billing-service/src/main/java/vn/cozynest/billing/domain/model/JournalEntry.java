package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JournalEntry {
    private final String id;
    private final String entryNumber;
    private final String description;
    private final String referenceType;
    private final String referenceId;
    private final String idempotencyKey;
    private final long totalAmount;
    private String status;
    private final String createdBy;
    private final Instant createdAt;
    private final List<LedgerPosting> postings;

    public JournalEntry(String id, String entryNumber, String description,
                        String referenceType, String referenceId, long totalAmount,
                        String status, String createdBy, Instant createdAt,
                        List<LedgerPosting> postings) {
        this(id, entryNumber, description, referenceType, referenceId, null,
                totalAmount, status, createdBy, createdAt, postings);
    }

    public JournalEntry(String id, String entryNumber, String description,
                        String referenceType, String referenceId, String idempotencyKey,
                        long totalAmount, String status, String createdBy, Instant createdAt,
                        List<LedgerPosting> postings) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.entryNumber = entryNumber;
        this.description = description;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.idempotencyKey = idempotencyKey;
        this.totalAmount = totalAmount;
        this.status = status != null ? status : "POSTED";
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.postings = postings != null ? new ArrayList<>(postings) : new ArrayList<>();
    }

    public static JournalEntry createAndValidate(String entryNumber, String description,
                                                 String referenceType, String referenceId,
                                                 String createdBy, List<LedgerPosting> postings) {
        return createWithIdAndIdempotency(null, entryNumber, description, referenceType, referenceId, null, createdBy, postings);
    }

    public static JournalEntry createWithIdAndIdempotency(String entryId, String entryNumber, String description,
                                                          String referenceType, String referenceId, String idempotencyKey,
                                                          String createdBy, List<LedgerPosting> postings) {
        if (postings == null || postings.size() < 2) {
            throw new IllegalArgumentException("But toan so cai phai co it nhat 2 dinh khoan (No va Co)");
        }
        long sumDebit = 0;
        long sumCredit = 0;
        for (LedgerPosting p : postings) {
            if (p.getDirection() == PostingDirection.DEBIT) {
                sumDebit += p.getAmount();
            } else if (p.getDirection() == PostingDirection.CREDIT) {
                sumCredit += p.getAmount();
            }
        }
        if (sumDebit != sumCredit) {
            throw new IllegalStateException("Quy tac can doi so cai bi vi pham: Tong No ("
                    + sumDebit + ") khac Tong Co (" + sumCredit + ")");
        }
        return new JournalEntry(
                entryId != null ? entryId : UUID.randomUUID().toString(),
                entryNumber, description, referenceType, referenceId, idempotencyKey,
                sumDebit, "POSTED", createdBy, Instant.now(), postings
        );
    }

    public void markReversed() {
        this.status = "REVERSED";
    }

    public String getId() { return id; }
    public String getEntryNumber() { return entryNumber; }
    public String getDescription() { return description; }
    public String getReferenceType() { return referenceType; }
    public String getReferenceId() { return referenceId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public long getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public List<LedgerPosting> getPostings() { return List.copyOf(postings); }
}
