package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.util.UUID;

public class PaymentIntent {
    private final String id;
    private final String paymentCode;
    private final String payerId;
    private final String orderType;
    private final String orderId;
    private final long amount;
    private long paidAmount;
    private String qrUrl;
    private String status;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public PaymentIntent(String id, String paymentCode, String payerId, String orderType,
                         String orderId, long amount, long paidAmount, String qrUrl,
                         String status, Instant expiresAt, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.paymentCode = paymentCode;
        this.payerId = payerId;
        this.orderType = orderType;
        this.orderId = orderId;
        this.amount = amount;
        this.paidAmount = paidAmount;
        this.qrUrl = qrUrl;
        this.status = status != null ? status : "PENDING";
        this.expiresAt = expiresAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static PaymentIntent create(String paymentCode, String payerId, String orderType,
                                       String orderId, long amount, String qrUrl, long ttlMinutes) {
        return new PaymentIntent(
                UUID.randomUUID().toString(),
                paymentCode,
                payerId,
                orderType,
                orderId,
                amount,
                0L,
                qrUrl,
                "PENDING",
                Instant.now().plusSeconds(ttlMinutes * 60),
                Instant.now(),
                Instant.now()
        );
    }

    public void markPaid(long actualAmount) {
        if ("PAID".equals(this.status)) {
            return; // Idempotent
        }
        if (Instant.now().isAfter(this.expiresAt)) {
            this.status = "EXPIRED";
            this.updatedAt = Instant.now();
            throw new IllegalStateException("Giao dich thanh toan da het han");
        }
        this.paidAmount = actualAmount;
        if (actualAmount < this.amount) {
            this.status = "MISMATCH";
        } else {
            this.status = "PAID";
        }
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = "CANCELLED";
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getPaymentCode() { return paymentCode; }
    public String getPayerId() { return payerId; }
    public String getOrderType() { return orderType; }
    public String getOrderId() { return orderId; }
    public long getAmount() { return amount; }
    public long getPaidAmount() { return paidAmount; }
    public String getQrUrl() { return qrUrl; }
    public String getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

