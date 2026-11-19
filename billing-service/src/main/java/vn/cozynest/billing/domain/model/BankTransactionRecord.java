package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.util.UUID;

public class BankTransactionRecord {
    private final String id;
    private final String normalizedTxCode;
    private final String provider;
    private final String direction;
    private final long amount;
    private final String referenceCode;
    private final String orderType;
    private final String orderId;
    private final String receiverAccountMasked;
    private final String rawContent;
    private final String sanitizedPayload;
    private final String recordedBy;
    private final Instant occurredAt;
    private final Instant createdAt;

    public BankTransactionRecord(String id, String normalizedTxCode, String provider, String direction,
                                 long amount, String referenceCode, String orderType, String orderId,
                                 String receiverAccountMasked, String rawContent, String sanitizedPayload,
                                 String recordedBy, Instant occurredAt, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.normalizedTxCode = PaymentReferenceCodeCodec.normalizeAlphanumericUpper(normalizedTxCode);
        this.provider = provider != null ? provider : "FAKE";
        this.direction = direction != null ? direction : "IN";
        this.amount = amount;
        this.referenceCode = referenceCode;
        this.orderType = orderType;
        this.orderId = orderId;
        this.receiverAccountMasked = receiverAccountMasked;
        this.rawContent = rawContent;
        this.sanitizedPayload = sanitizedPayload;
        this.recordedBy = recordedBy != null ? recordedBy : "SYSTEM";
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static BankTransactionRecord createInflow(String txCode, String provider, long amount,
                                                     String referenceCode, String orderType, String orderId,
                                                     String receiverAccountMasked, String rawContent,
                                                     String sanitizedPayload, String recordedBy, Instant occurredAt) {
        return new BankTransactionRecord(UUID.randomUUID().toString(), txCode, provider, "IN",
                amount, referenceCode, orderType, orderId, receiverAccountMasked, rawContent,
                sanitizedPayload, recordedBy, occurredAt, Instant.now());
    }

    public static BankTransactionRecord createOutflow(String txCode, String provider, long amount,
                                                      String referenceCode, String orderType, String orderId,
                                                      String receiverAccountMasked, String rawContent,
                                                      String recordedBy, Instant occurredAt) {
        return new BankTransactionRecord(UUID.randomUUID().toString(), txCode, provider, "OUT",
                amount, referenceCode, orderType, orderId, receiverAccountMasked, rawContent,
                null, recordedBy, occurredAt, Instant.now());
    }

    public String getId() { return id; }
    public String getNormalizedTxCode() { return normalizedTxCode; }
    public String getProvider() { return provider; }
    public String getDirection() { return direction; }
    public long getAmount() { return amount; }
    public String getReferenceCode() { return referenceCode; }
    public String getOrderType() { return orderType; }
    public String getOrderId() { return orderId; }
    public String getReceiverAccountMasked() { return receiverAccountMasked; }
    public String getRawContent() { return rawContent; }
    public String getSanitizedPayload() { return sanitizedPayload; }
    public String getRecordedBy() { return recordedBy; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getCreatedAt() { return createdAt; }
}

