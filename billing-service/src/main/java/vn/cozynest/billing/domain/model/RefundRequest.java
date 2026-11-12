package vn.cozynest.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {
    private String id;
    private String refundCode;
    private String paymentIntentId;
    private String contractId;
    private String tenantId;
    private String landlordId;
    private long amount;
    private String bankCode;
    private String accountNumber;
    private String accountHolder;
    private boolean otpVerified;
    private boolean kycNameMatched;
    private boolean requiresMakerChecker;
    private String makerCheckerRequestId;
    private String status;
    private String reason;
    private String failureReason;
    private int retryCount;
    private Instant createdAt;
    private Instant updatedAt;

    public static RefundRequest create(
            String refundCode, String paymentIntentId, String contractId,
            String tenantId, String landlordId, long amount,
            String bankCode, String accountNumber, String accountHolder,
            boolean otpVerified, boolean kycNameMatched,
            boolean requiresMakerChecker, String makerCheckerRequestId, String reason
    ) {
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien hoan tra phai lon hon 0");
        }
        return RefundRequest.builder()
                .id(UUID.randomUUID().toString())
                .refundCode(refundCode)
                .paymentIntentId(paymentIntentId)
                .contractId(contractId)
                .tenantId(tenantId)
                .landlordId(landlordId)
                .amount(amount)
                .bankCode(bankCode)
                .accountNumber(accountNumber)
                .accountHolder(accountHolder)
                .otpVerified(otpVerified)
                .kycNameMatched(kycNameMatched)
                .requiresMakerChecker(requiresMakerChecker)
                .makerCheckerRequestId(makerCheckerRequestId)
                .status(requiresMakerChecker ? "PENDING_APPROVAL" : "REQUESTED")
                .reason(reason)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public void markProcessing() {
        this.status = "PROCESSING";
        this.updatedAt = Instant.now();
    }

    public void markCompleted() {
        this.status = "COMPLETED";
        this.failureReason = null;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = "FAILED";
        this.failureReason = reason;
        this.retryCount++;
        this.updatedAt = Instant.now();
    }

    public void markRejected(String rejectReason) {
        this.status = "REJECTED";
        if (rejectReason != null) {
            this.reason = rejectReason;
        }
        this.updatedAt = Instant.now();
    }
}

