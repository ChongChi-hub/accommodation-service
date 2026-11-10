package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.util.UUID;

public class PayoutRequest {
    private final String id;
    private final String payoutCode;
    private final String landlordId;
    private final long amount;
    private final String bankCode;
    private final String accountNumber;
    private final String accountHolder;
    private String status;
    private boolean otpVerified;
    private String failureReason;
    private final Instant createdAt;
    private Instant updatedAt;

    public PayoutRequest(String id, String payoutCode, String landlordId, long amount,
                         String bankCode, String accountNumber, String accountHolder,
                         String status, boolean otpVerified, String failureReason,
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
        this.failureReason = failureReason;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static PayoutRequest create(String payoutCode, String landlordId, long amount,
                                       String bankCode, String accountNumber, String accountHolder,
                                       boolean otpVerified) {
        return new PayoutRequest(
                UUID.randomUUID().toString(),
                payoutCode,
                landlordId,
                amount,
                bankCode,
                accountNumber,
                accountHolder,
                "REQUESTED",
                otpVerified,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    public void approve() {
        this.status = "APPROVED";
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.status = "COMPLETED";
        this.updatedAt = Instant.now();
    }

    public void reject(String reason) {
        this.status = "REJECTED";
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getPayoutCode() { return payoutCode; }
    public String getLandlordId() { return landlordId; }
    public long getAmount() { return amount; }
    public String getBankCode() { return bankCode; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolder() { return accountHolder; }
    public String getStatus() { return status; }
    public boolean isOtpVerified() { return otpVerified; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

