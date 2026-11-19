package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.util.UUID;

public class PayoutRequest {
    private final String id;
    private final String payoutCode;
    private String idempotencyKey;
    private final String landlordId;
    private final long amount;
    private final String bankCode;
    private final String accountNumber;
    private final String accountHolder;
    private String status;
    private boolean otpVerified;
    private boolean requiresMakerChecker;
    private String makerCheckerRequestId;
    private String failureReason;
    private String bankTxCode;
    private String proofUrl;
    private String approvedBy;
    private String paidBy;
    private Instant paidAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public PayoutRequest(String id, String payoutCode, String landlordId, long amount,
                         String bankCode, String accountNumber, String accountHolder,
                         String status, boolean otpVerified, String failureReason,
                         Instant createdAt, Instant updatedAt) {
        this(id, payoutCode, landlordId, amount, bankCode, accountNumber, accountHolder,
                status, otpVerified, false, null, failureReason, createdAt, updatedAt);
    }

    public PayoutRequest(String id, String payoutCode, String landlordId, long amount,
                         String bankCode, String accountNumber, String accountHolder,
                         String status, boolean otpVerified, boolean requiresMakerChecker,
                         String makerCheckerRequestId, String failureReason,
                         Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.payoutCode = payoutCode;
        this.landlordId = landlordId;
        this.amount = amount;
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.status = status != null ? status : "REQUESTED";
        this.otpVerified = otpVerified;
        this.requiresMakerChecker = requiresMakerChecker;
        this.makerCheckerRequestId = makerCheckerRequestId;
        this.failureReason = failureReason;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static PayoutRequest create(String payoutCode, String landlordId, long amount,
                                       String bankCode, String accountNumber, String accountHolder,
                                       boolean otpVerified) {
        return create(payoutCode, landlordId, amount, bankCode, accountNumber, accountHolder, otpVerified, false, null);
    }

    public static PayoutRequest create(String payoutCode, String landlordId, long amount,
                                       String bankCode, String accountNumber, String accountHolder,
                                       boolean otpVerified, boolean requiresMakerChecker, String makerCheckerRequestId) {
        return new PayoutRequest(UUID.randomUUID().toString(), payoutCode, landlordId, amount,
                bankCode, accountNumber, accountHolder, requiresMakerChecker ? "PENDING_APPROVAL" : "REQUESTED",
                otpVerified, requiresMakerChecker, makerCheckerRequestId, null, Instant.now(), Instant.now());
    }

    public void attachExtendedFields(String idempotencyKey, String bankTxCode, String proofUrl,
                                     String approvedBy, String paidBy, Instant paidAt) {
        this.idempotencyKey = idempotencyKey;
        this.bankTxCode = bankTxCode;
        this.proofUrl = proofUrl;
        this.approvedBy = approvedBy;
        this.paidBy = paidBy;
        this.paidAt = paidAt;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public void approve() {
        this.status = "APPROVED";
        this.updatedAt = Instant.now();
    }

    public void approveBy(String adminId) {
        this.status = "APPROVED";
        this.approvedBy = adminId;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.status = "COMPLETED";
        this.paidAt = Instant.now();
        this.updatedAt = this.paidAt;
    }

    public void markPaid(String bankTxCode, String proofUrl, String adminId) {
        this.status = "PAID";
        this.bankTxCode = bankTxCode;
        this.proofUrl = proofUrl;
        this.paidBy = adminId;
        this.paidAt = Instant.now();
        this.updatedAt = this.paidAt;
    }

    public void cancelByLandlord() {
        if (!"REQUESTED".equals(this.status) && !"PENDING_APPROVAL".equals(this.status)) {
            throw new IllegalStateException("Chỉ có thể hủy yêu cầu rút tiền khi chưa được duyệt (trạng thái hiện tại: " + this.status + ")");
        }
        this.status = "CANCELLED";
        this.updatedAt = Instant.now();
    }

    public void fail(String reason) {
        this.status = "FAILED";
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public void reject(String reason) {
        this.status = "REJECTED";
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getPayoutCode() { return payoutCode; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getLandlordId() { return landlordId; }
    public long getAmount() { return amount; }
    public String getBankCode() { return bankCode; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolder() { return accountHolder; }
    public String getStatus() { return status; }
    public boolean isOtpVerified() { return otpVerified; }
    public boolean isRequiresMakerChecker() { return requiresMakerChecker; }
    public String getMakerCheckerRequestId() { return makerCheckerRequestId; }
    public String getFailureReason() { return failureReason; }
    public String getBankTxCode() { return bankTxCode; }
    public String getProofUrl() { return proofUrl; }
    public String getApprovedBy() { return approvedBy; }
    public String getPaidBy() { return paidBy; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
