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
    private int missingInternalTx;
    private int missingProviderTx;
    private String status;
    private String summaryNotes;
    private Instant slaDueAt;
    private String assignedTo;
    private String resolvedBy;
    private Instant resolvedAt;
    private final Instant createdAt;

    public ReconciliationReport(String id, LocalDate reconDate, int totalInternalTx,
                                int totalGatewayTx, int matchedTx, int mismatchedTx,
                                String status, String summaryNotes, Instant createdAt) {
        this(id, reconDate, totalInternalTx, totalGatewayTx, matchedTx, mismatchedTx,
                0, 0, status, summaryNotes, null, null, null, null, createdAt);
    }

    public ReconciliationReport(String id, LocalDate reconDate, int totalInternalTx,
                                int totalGatewayTx, int matchedTx, int mismatchedTx,
                                int missingInternalTx, int missingProviderTx,
                                String status, String summaryNotes,
                                Instant slaDueAt, String assignedTo, String resolvedBy, Instant resolvedAt,
                                Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.reconDate = reconDate;
        this.totalInternalTx = totalInternalTx;
        this.totalGatewayTx = totalGatewayTx;
        this.matchedTx = matchedTx;
        this.mismatchedTx = mismatchedTx;
        this.missingInternalTx = missingInternalTx;
        this.missingProviderTx = missingProviderTx;
        this.status = status != null ? status : "COMPLETED";
        this.summaryNotes = summaryNotes;
        this.slaDueAt = slaDueAt;
        this.assignedTo = assignedTo;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
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
                0,
                0,
                status,
                summaryNotes,
                mismatchedTx > 0 ? Instant.now().plusSeconds(24 * 3600L) : null,
                null,
                null,
                null,
                Instant.now()
        );
    }

    public void update(int totalInternalTx, int matchedTx, int mismatchedTx, String status, String summaryNotes) {
        this.totalInternalTx = totalInternalTx;
        this.matchedTx = matchedTx;
        this.mismatchedTx = mismatchedTx;
        this.status = status;
        this.summaryNotes = summaryNotes;
        if (mismatchedTx > 0 && this.slaDueAt == null) {
            this.slaDueAt = Instant.now().plusSeconds(24 * 3600L);
        }
    }

    public void updateGatewayCounts(int totalGatewayTx, int missingInternalTx, int missingProviderTx) {
        this.totalGatewayTx = totalGatewayTx;
        this.missingInternalTx = missingInternalTx;
        this.missingProviderTx = missingProviderTx;
    }

    public void assignTo(String adminId) {
        this.assignedTo = adminId;
    }

    public void markResolved(String resolverId, String notes) {
        this.resolvedBy = resolverId;
        this.resolvedAt = Instant.now();
        this.status = "RESOLVED";
        if (notes != null && !notes.isBlank()) {
            this.summaryNotes = (this.summaryNotes != null ? this.summaryNotes + " | RESOLVED: " : "RESOLVED: ") + notes;
        }
    }

    public String getId() { return id; }
    public LocalDate getReconDate() { return reconDate; }
    public int getTotalInternalTx() { return totalInternalTx; }
    public int getTotalGatewayTx() { return totalGatewayTx; }
    public int getMatchedTx() { return matchedTx; }
    public int getMismatchedTx() { return mismatchedTx; }
    public int getMissingInternalTx() { return missingInternalTx; }
    public int getMissingProviderTx() { return missingProviderTx; }
    public String getStatus() { return status; }
    public String getSummaryNotes() { return summaryNotes; }
    public Instant getSlaDueAt() { return slaDueAt; }
    public String getAssignedTo() { return assignedTo; }
    public String getResolvedBy() { return resolvedBy; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getCreatedAt() { return createdAt; }
}

