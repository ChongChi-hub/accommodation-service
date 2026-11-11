package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ReconciliationReport {
    private final String id;
    private final LocalDate reconDate;
    private int totalInternalTx;
    private int totalGatewayTx;
    private int matchedTx;
    private int mismatchedTx;
    private String status;
    private String summaryNotes;
    private final Instant createdAt;

    public ReconciliationReport(String id, LocalDate reconDate, int totalInternalTx,
                                int totalGatewayTx, int matchedTx, int mismatchedTx,
                                String status, String summaryNotes, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.reconDate = reconDate;
        this.totalInternalTx = totalInternalTx;
        this.totalGatewayTx = totalGatewayTx;
        this.matchedTx = matchedTx;
        this.mismatchedTx = mismatchedTx;
        this.status = status != null ? status : "COMPLETED";
        this.summaryNotes = summaryNotes;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static ReconciliationReport create(LocalDate reconDate, int totalInternalTx,
                                              int matchedTx, int mismatchedTx,
                                              String status, String summaryNotes) {
        return new ReconciliationReport(
                UUID.randomUUID().toString(),
                reconDate,
                totalInternalTx,
                0,
                matchedTx,
                mismatchedTx,
                status,
                summaryNotes,
                Instant.now()
        );
    }

    public void update(int totalInternalTx, int matchedTx, int mismatchedTx, String status, String summaryNotes) {
        this.totalInternalTx = totalInternalTx;
        this.matchedTx = matchedTx;
        this.mismatchedTx = mismatchedTx;
        this.status = status;
        this.summaryNotes = summaryNotes;
    }

    public String getId() { return id; }
    public LocalDate getReconDate() { return reconDate; }
    public int getTotalInternalTx() { return totalInternalTx; }
    public int getTotalGatewayTx() { return totalGatewayTx; }
    public int getMatchedTx() { return matchedTx; }
    public int getMismatchedTx() { return mismatchedTx; }
    public String getStatus() { return status; }
    public String getSummaryNotes() { return summaryNotes; }
    public Instant getCreatedAt() { return createdAt; }
}

