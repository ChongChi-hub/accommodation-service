package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class EscrowHold {
    private final String id;
    private final String contractId;
    private final String tenantId;
    private final String landlordId;
    private final long amount;
    private String status;
    private final boolean sandboxMode;
    private final Instant inspectionEndsAt;
    private String disputeReason;
    private Instant releasedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public EscrowHold(String id, String contractId, String tenantId, String landlordId, long amount,
                      String status, boolean sandboxMode, Instant inspectionEndsAt, String disputeReason,
                      Instant releasedAt, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.contractId = contractId;
        this.tenantId = tenantId;
        this.landlordId = landlordId;
        this.amount = amount;
        this.status = status != null ? status : "HELD";
        this.sandboxMode = sandboxMode;
        this.inspectionEndsAt = inspectionEndsAt != null ? inspectionEndsAt : Instant.now().plus(72, ChronoUnit.HOURS);
        this.disputeReason = disputeReason;
        this.releasedAt = releasedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static EscrowHold createHold(String contractId, String tenantId, String landlordId,
                                        long amount, boolean sandboxMode, long inspectionHours) {
        if (amount <= 0) throw new IllegalArgumentException("So tien ky quy phai lon hon 0");
        Instant now = Instant.now();
        return new EscrowHold(UUID.randomUUID().toString(), contractId, tenantId, landlordId,
                amount, "HELD", sandboxMode, now.plus(Math.max(1, inspectionHours), ChronoUnit.HOURS),
                null, null, now, now);
    }

    public void freezeForDispute(String reason) {
        if ("RELEASED".equals(status) || "REFUNDED".equals(status)) {
            throw new IllegalStateException("Khong the dong bang khoan ky quy da tat toan");
        }
        this.status = "DISPUTE_FROZEN";
        this.disputeReason = reason;
        this.updatedAt = Instant.now();
    }

    public void unfreezeDispute() {
        if (!"DISPUTE_FROZEN".equals(status)) {
            throw new IllegalStateException("Khoan ky quy khong o trang thai dong bang tranh chap");
        }
        this.status = "HELD";
        this.disputeReason = null;
        this.updatedAt = Instant.now();
    }

    public void release(boolean forceBeforeInspection) {
        if ("DISPUTE_FROZEN".equals(status)) {
            throw new IllegalStateException("Khoan ky quy dang bi dong bang do tranh chap");
        }
        if (!"HELD".equals(status)) {
            throw new IllegalStateException("Trang thai ky quy khong hop le de giai ngan: " + status);
        }
        if (!forceBeforeInspection && Instant.now().isBefore(inspectionEndsAt)) {
            throw new IllegalStateException("Chua het thoi gian kiem tra 72 gio truoc khi giai ngan tu dong");
        }
        this.status = "RELEASED";
        this.releasedAt = Instant.now();
        this.updatedAt = this.releasedAt;
    }

    public void refund(String reason) {
        if (!"HELD".equals(status) && !"DISPUTE_FROZEN".equals(status)) {
            throw new IllegalStateException("Trang thai ky quy khong hop le de hoan tien: " + status);
        }
        this.status = "REFUNDED";
        this.disputeReason = reason;
        this.releasedAt = Instant.now();
        this.updatedAt = this.releasedAt;
    }

    public String getId() { return id; }
    public String getContractId() { return contractId; }
    public String getTenantId() { return tenantId; }
    public String getLandlordId() { return landlordId; }
    public long getAmount() { return amount; }
    public String getStatus() { return status; }
    public boolean isSandboxMode() { return sandboxMode; }
    public Instant getInspectionEndsAt() { return inspectionEndsAt; }
    public String getDisputeReason() { return disputeReason; }
    public Instant getReleasedAt() { return releasedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

