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
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien thanh toan phai lon hon 0");
        }
        return new PaymentIntent(
                UUID.randomUUID().toString(), paymentCode, payerId, orderType, orderId,
                amount, 0L, qrUrl, "PENDING",
                Instant.now().plusSeconds(ttlMinutes * 60), Instant.now(), Instant.now());
    }

    public PaymentOutcome recordGatewayPayment(long actualAmount, Instant now) {
        if (actualAmount <= 0) {
            throw new IllegalArgumentException("So tien nhan tu webhook phai lon hon 0");
        }
        if ("PAID".equals(this.status) || "OVERPAID".equals(this.status)) {
            return PaymentOutcome.IDEMPOTENT_ALREADY_PAID;
        }
        if ("CANCELLED".equals(this.status)) {
            this.status = "MISMATCH";
            this.updatedAt = now;
            return PaymentOutcome.LATE_AFTER_CANCEL;
        }
        if ("EXPIRED".equals(this.status) || now.isAfter(this.expiresAt)) {
            this.status = "EXPIRED";
            this.paidAmount += actualAmount;
            this.updatedAt = now;
            return PaymentOutcome.LATE_EXPIRED;
        }
        this.paidAmount += actualAmount;
        this.updatedAt = now;
        if (this.paidAmount < this.amount) {
            this.status = "PARTIAL_PAID";
            return PaymentOutcome.UNDERPAID;
        }
        if (this.paidAmount > this.amount) {
            this.status = "OVERPAID";
            return PaymentOutcome.OVERPAID;
        }
        this.status = "PAID";
        return PaymentOutcome.SETTLED_EXACT;
    }

    public void markPaid(long actualAmount) {
        PaymentOutcome outcome = recordGatewayPayment(actualAmount, Instant.now());
        if (outcome == PaymentOutcome.LATE_EXPIRED || outcome == PaymentOutcome.LATE_AFTER_CANCEL) {
            throw new IllegalStateException("Giao dich thanh toan da het han hoac da bi huy");
        }
    }

    public boolean markExpiredIfPast(Instant now) {
        if ("PENDING".equals(this.status) && now.isAfter(this.expiresAt)) {
            this.status = "EXPIRED";
            this.updatedAt = now;
            return true;
        }
        return false;
    }

    public void cancel() {
        if ("PAID".equals(this.status) || "OVERPAID".equals(this.status)) {
            throw new IllegalStateException("Khong the huy giao dich da thanh toan");
        }
        this.status = "CANCELLED";
        this.updatedAt = Instant.now();
    }

    public enum PaymentOutcome {
        SETTLED_EXACT,
        UNDERPAID,
        OVERPAID,
        LATE_EXPIRED,
        LATE_AFTER_CANCEL,
        IDEMPOTENT_ALREADY_PAID
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
