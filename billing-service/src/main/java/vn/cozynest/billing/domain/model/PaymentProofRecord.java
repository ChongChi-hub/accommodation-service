package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.util.UUID;

public class PaymentProofRecord {
    private final String id;
    private final String paymentCode;
    private final String orderType;
    private final String orderId;
    private final String uploaderId;
    private final String normalizedBankTxCode;
    private final String sha256Hash;
    private final String dhash;
    private final boolean nearDuplicateFlag;
    private final String nearDuplicateOfId;
    private final String mimeType;
    private final String storageKey;
    private final Instant createdAt;

    public PaymentProofRecord(String id, String paymentCode, String orderType, String orderId,
                              String uploaderId, String normalizedBankTxCode, String sha256Hash,
                              String dhash, boolean nearDuplicateFlag, String nearDuplicateOfId,
                              String mimeType, String storageKey, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.paymentCode = paymentCode;
        this.orderType = orderType;
        this.orderId = orderId;
        this.uploaderId = uploaderId;
        this.normalizedBankTxCode = PaymentReferenceCodeCodec.normalizeAlphanumericUpper(normalizedBankTxCode);
        this.sha256Hash = sha256Hash != null ? sha256Hash.toLowerCase() : "";
        this.dhash = dhash != null ? dhash.toLowerCase() : "0000000000000000";
        this.nearDuplicateFlag = nearDuplicateFlag;
        this.nearDuplicateOfId = nearDuplicateOfId;
        this.mimeType = mimeType != null ? mimeType : "image/jpeg";
        this.storageKey = storageKey;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static int computeDhashHammingDistance(String h1, String h2) {
        if (h1 == null || h2 == null || h1.length() != 16 || h2.length() != 16) {
            return 64;
        }
        try {
            long v1 = Long.parseUnsignedLong(h1, 16);
            long v2 = Long.parseUnsignedLong(h2, 16);
            return Long.bitCount(v1 ^ v2);
        } catch (NumberFormatException e) {
            return 64;
        }
    }

    public String getId() { return id; }
    public String getPaymentCode() { return paymentCode; }
    public String getOrderType() { return orderType; }
    public String getOrderId() { return orderId; }
    public String getUploaderId() { return uploaderId; }
    public String getNormalizedBankTxCode() { return normalizedBankTxCode; }
    public String getSha256Hash() { return sha256Hash; }
    public String getDhash() { return dhash; }
    public boolean isNearDuplicateFlag() { return nearDuplicateFlag; }
    public String getNearDuplicateOfId() { return nearDuplicateOfId; }
    public String getMimeType() { return mimeType; }
    public String getStorageKey() { return storageKey; }
    public Instant getCreatedAt() { return createdAt; }
}

