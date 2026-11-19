package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class EscrowHold {
    private final String id;
    private final String contractId;
    private final String tenantId;
    private final String landlordId;
    private long amount;
    private long releasedAmount;
    private long retainedAmount;
    private String status;
    private final boolean sandboxMode;
    private boolean tenantConfirmedMoveIn;
    private boolean landlordConfirmedMoveIn;
    private Instant moveInConfirmedAt;
    private Instant pendingReleaseUntil;
    private boolean pendingSettled;
    private Instant inspectionEndsAt;
    private String disputeReason;
    private String paymentCode;
    private String bankTxCode;
    private String receiptHash;
    private String receiptUrl;
    private Long deductionClaimAmount;
    private String deductionClaimReason;
    private String deductionClaimProofUrl;
    private String deductionClaimStatus;
    private Instant releasedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public EscrowHold(String id, String contractId, String tenantId, String landlordId, long amount,
                      String status, boolean sandboxMode, Instant inspectionEndsAt, String disputeReason,
                      Instant releasedAt, Instant createdAt, Instant updatedAt) {
        this(id, contractId, tenantId, landlordId, amount, status, sandboxMode,
                false, false, null, inspectionEndsAt, disputeReason, releasedAt, createdAt, updatedAt);
    }

    public EscrowHold(String id, String contractId, String tenantId, String landlordId, long amount,
                      String status, boolean sandboxMode, boolean tenantConfirmedMoveIn,
                      boolean landlordConfirmedMoveIn, Instant moveInConfirmedAt,
                      Instant inspectionEndsAt, String disputeReason,
                      Instant releasedAt, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.contractId = contractId;
        this.tenantId = tenantId;
        this.landlordId = landlordId;
        this.amount = amount;
        this.status = status != null ? status : "HELD";
        this.releasedAmount = "RELEASED".equals(this.status) ? amount : 0L;
        this.retainedAmount = ("HELD".equals(this.status) || "DISPUTE_FROZEN".equals(this.status)) ? amount : 0L;
        this.sandboxMode = sandboxMode;
        this.tenantConfirmedMoveIn = tenantConfirmedMoveIn;
        this.landlordConfirmedMoveIn = landlordConfirmedMoveIn;
        this.moveInConfirmedAt = moveInConfirmedAt;
        this.inspectionEndsAt = inspectionEndsAt != null ? inspectionEndsAt : Instant.now().plus(72, ChronoUnit.HOURS);
        this.disputeReason = disputeReason;
        this.deductionClaimStatus = "NONE";
        this.releasedAt = releasedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static EscrowHold createHold(String contractId, String tenantId, String landlordId,
                                        long amount, boolean sandboxMode, long inspectionHours) {
        if (amount <= 0) throw new IllegalArgumentException("So tien ky quy phai lon hon 0");
        Instant now = Instant.now();
        return new EscrowHold(UUID.randomUUID().toString(), contractId, tenantId, landlordId,
                amount, "HELD", sandboxMode, now.plus(Math.max(1, inspectionHours), ChronoUnit.HOURS), null, null, now, now);
    }

    public static EscrowHold createPendingHold(String contractId, String tenantId, String landlordId,
                                               long amount, boolean sandboxMode, long inspectionHours) {
        if (amount <= 0) throw new IllegalArgumentException("So tien ky quy phai lon hon 0");
        Instant now = Instant.now();
        return new EscrowHold(UUID.randomUUID().toString(), contractId, tenantId, landlordId,
                amount, "AWAITING_DEPOSIT", sandboxMode, now.plus(Math.max(1, inspectionHours), ChronoUnit.HOURS), null, null, now, now);
    }

    public void activateFromDeposit(long paidAmount, long inspectionHours) {
        if ("AWAITING_DEPOSIT".equals(status) || "CANCELLED".equals(status)) {
            if (paidAmount > 0) this.amount = paidAmount;
            this.status = "HELD";
            this.releasedAmount = 0L;
            this.retainedAmount = this.amount;
            Instant now = Instant.now();
            this.inspectionEndsAt = now.plus(Math.max(1, inspectionHours), ChronoUnit.HOURS);
            this.updatedAt = now;
        }
    }

    public void cancelPending(String reason) {
        if (!"AWAITING_DEPOSIT".equals(status)) throw new IllegalStateException("Chi co the huy khoan ky quy chua thanh toan");
        this.status = "CANCELLED";
        this.disputeReason = reason;
        this.updatedAt = Instant.now();
    }

    public void freezeForDispute(String reason) {
        if ("RELEASED".equals(status) || "REFUNDED".equals(status)) throw new IllegalStateException("Khong the dong bang khoan ky quy da tat toan");
        this.status = "DISPUTE_FROZEN";
        this.disputeReason = reason;
        this.updatedAt = Instant.now();
    }

    public void unfreezeDispute() {
        if (!"DISPUTE_FROZEN".equals(status)) throw new IllegalStateException("Khoan ky quy khong o trang thai dong bang tranh chap");
        this.status = (this.releasedAmount > 0 && this.retainedAmount > 0) ? "PARTIALLY_RELEASED" : "HELD";
        this.disputeReason = null;
        this.updatedAt = Instant.now();
    }

    public void release(boolean forceBeforeInspection) {
        if ("DISPUTE_FROZEN".equals(status)) throw new IllegalStateException("Khoan ky quy dang bi dong bang do tranh chap");
        if (!"HELD".equals(status) && !"PARTIALLY_RELEASED".equals(status)) throw new IllegalStateException("Trang thai ky quy khong hop le de giai ngan: " + status);
        if (!forceBeforeInspection && Instant.now().isBefore(inspectionEndsAt)) throw new IllegalStateException("Chua het thoi gian kiem tra 72 gio truoc khi giai ngan tu dong");
        this.releasedAmount = this.amount;
        this.retainedAmount = 0L;
        this.status = "RELEASED";
        this.releasedAt = Instant.now();
        this.updatedAt = this.releasedAt;
    }

    public void refund(String reason) {
        if (!"HELD".equals(status) && !"PARTIALLY_RELEASED".equals(status) && !"DISPUTE_FROZEN".equals(status)) {
            throw new IllegalStateException("Trang thai ky quy khong hop le de hoan tien: " + status);
        }
        this.retainedAmount = 0L;
        this.status = "REFUNDED";
        this.disputeReason = reason;
        this.releasedAt = Instant.now();
        this.updatedAt = this.releasedAt;
    }

    public void confirmMoveIn(boolean isTenant, boolean isLandlord) {
        if ("AWAITING_DEPOSIT".equals(status) || "CANCELLED".equals(status) || "DISPUTE_FROZEN".equals(status) || "RELEASED".equals(status) || "REFUNDED".equals(status)) {
            throw new IllegalStateException("Trang thai ky quy khong cho phep xac nhan nhan phong: " + status);
        }
        if (isTenant) this.tenantConfirmedMoveIn = true;
        if (isLandlord) this.landlordConfirmedMoveIn = true;
        if (this.tenantConfirmedMoveIn && this.landlordConfirmedMoveIn && this.moveInConfirmedAt == null) this.moveInConfirmedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public long applyPartialReleaseOnMoveIn(int releasePercent, int holdDays) {
        if (!isBothConfirmedMoveIn() || this.releasedAmount > 0) return 0L;
        int pct = Math.max(0, Math.min(100, releasePercent));
        long toRelease = (this.amount * pct) / 100L;
        this.releasedAmount = toRelease;
        this.retainedAmount = this.amount - toRelease;
        Instant now = Instant.now();
        this.pendingReleaseUntil = holdDays > 0 ? now.plus(holdDays, ChronoUnit.DAYS) : now;
        this.pendingSettled = holdDays <= 0;
        if (this.retainedAmount == 0L) { this.status = "RELEASED"; this.releasedAt = now; }
        else if (toRelease > 0L) { this.status = "PARTIALLY_RELEASED"; }
        this.updatedAt = now;
        return toRelease;
    }

    public void markPendingSettled() { this.pendingSettled = true; this.updatedAt = Instant.now(); }

    public void submitDeductionClaim(long claimAmount, String reason, String proofUrl) {
        long maxRetained = getEffectiveRetainedAmount();
        if (claimAmount <= 0 || claimAmount > maxRetained) throw new IllegalArgumentException("So tien khau tru phai > 0 va <= phan coc dang giu (" + maxRetained + " VND)");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Bat buoc nhap ly do khau tru coc");
        this.deductionClaimAmount = claimAmount;
        this.deductionClaimReason = reason.trim();
        this.deductionClaimProofUrl = proofUrl;
        this.deductionClaimStatus = "CLAIMED";
        this.updatedAt = Instant.now();
    }

    public long decideDeductionClaim(boolean approved, String note) {
        if (!"CLAIMED".equals(this.deductionClaimStatus)) throw new IllegalStateException("Khong co yeu cau khau tru coc nao dang cho duyet");
        this.updatedAt = Instant.now();
        if (approved) {
            long deducted = this.deductionClaimAmount != null ? this.deductionClaimAmount : 0L;
            this.releasedAmount += deducted;
            this.retainedAmount = Math.max(0L, getEffectiveRetainedAmount() - deducted);
            this.deductionClaimStatus = "APPROVED";
            if (this.retainedAmount == 0L) { this.status = "RELEASED"; this.releasedAt = this.updatedAt; }
            return deducted;
        }
        this.deductionClaimStatus = "REJECTED";
        this.disputeReason = note;
        return 0L;
    }

    public long getEffectiveRetainedAmount() {
        if ("RELEASED".equals(status) || "REFUNDED".equals(status) || "AWAITING_DEPOSIT".equals(status) || "CANCELLED".equals(status)) {
            return 0L;
        }
        return this.retainedAmount > 0 ? this.retainedAmount : (this.amount - this.releasedAmount);
    }

    public boolean isBothConfirmedMoveIn() { return tenantConfirmedMoveIn && landlordConfirmedMoveIn; }

    public void attachPaymentProof(String paymentCode, String bankTxCode, String receiptHash, String receiptUrl) {
        this.paymentCode = paymentCode;
        this.bankTxCode = bankTxCode;
        this.receiptHash = receiptHash;
        this.receiptUrl = receiptUrl;
        this.updatedAt = Instant.now();
    }

    public void configurePartialFields(long releasedAmount, long retainedAmount, Instant pendingReleaseUntil,
                                       boolean pendingSettled, Long deductionClaimAmount, String deductionClaimReason,
                                       String deductionClaimProofUrl, String deductionClaimStatus) {
        this.releasedAmount = releasedAmount;
        this.retainedAmount = retainedAmount;
        this.pendingReleaseUntil = pendingReleaseUntil;
        this.pendingSettled = pendingSettled;
        this.deductionClaimAmount = deductionClaimAmount;
        this.deductionClaimReason = deductionClaimReason;
        this.deductionClaimProofUrl = deductionClaimProofUrl;
        this.deductionClaimStatus = deductionClaimStatus != null ? deductionClaimStatus : "NONE";
    }

    public void rejectInvalidProof(String reason) {
        this.status = "AWAITING_DEPOSIT";
        this.tenantConfirmedMoveIn = false;
        this.landlordConfirmedMoveIn = false;
        this.moveInConfirmedAt = null;
        this.releasedAmount = 0L;
        this.retainedAmount = 0L;
        this.disputeReason = reason;
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; } public String getContractId() { return contractId; }
    public String getTenantId() { return tenantId; } public String getLandlordId() { return landlordId; }
    public long getAmount() { return amount; } public long getReleasedAmount() { return releasedAmount; }
    public long getRetainedAmount() { return getEffectiveRetainedAmount(); } public String getStatus() { return status; }
    public boolean isSandboxMode() { return sandboxMode; } public boolean isTenantConfirmedMoveIn() { return tenantConfirmedMoveIn; }
    public boolean isLandlordConfirmedMoveIn() { return landlordConfirmedMoveIn; } public Instant getMoveInConfirmedAt() { return moveInConfirmedAt; }
    public Instant getPendingReleaseUntil() { return pendingReleaseUntil; } public boolean isPendingSettled() { return pendingSettled; }
    public Instant getInspectionEndsAt() { return inspectionEndsAt; } public String getDisputeReason() { return disputeReason; }
    public String getPaymentCode() { return paymentCode; } public String getBankTxCode() { return bankTxCode; }
    public String getReceiptHash() { return receiptHash; } public String getReceiptUrl() { return receiptUrl; }
    public Long getDeductionClaimAmount() { return deductionClaimAmount; } public String getDeductionClaimReason() { return deductionClaimReason; }
    public String getDeductionClaimProofUrl() { return deductionClaimProofUrl; } public String getDeductionClaimStatus() { return deductionClaimStatus; }
    public Instant getReleasedAt() { return releasedAt; } public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
