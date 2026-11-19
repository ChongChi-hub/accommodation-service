package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.util.UUID;

public class UnmatchedBankInflow {
    private final String id;
    private final String provider;
    private final String providerTxId;
    private final long amount;
    private final String rawContent;
    private final String extractedReference;
    private final String mismatchReason;
    private String matchedPaymentCode;
    private final String receiverAccountMasked;
    private final String sanitizedPayload;
    private String status;
    private String resolvedBy;
    private String resolutionNote;
    private Instant resolvedAt;
    private final Instant receivedAt;

    public UnmatchedBankInflow(String id, String provider, String providerTxId, long amount,
                               String rawContent, String extractedReference, String mismatchReason,
                               String matchedPaymentCode, String receiverAccountMasked, String sanitizedPayload,
                               String status, String resolvedBy, String resolutionNote,
                               Instant resolvedAt, Instant receivedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.provider = provider != null ? provider : "FAKE";
        this.providerTxId = PaymentReferenceCodeCodec.normalizeAlphanumericUpper(providerTxId);
        this.amount = amount;
        this.rawContent = rawContent;
        this.extractedReference = extractedReference;
        this.mismatchReason = mismatchReason;
        this.matchedPaymentCode = matchedPaymentCode;
        this.receiverAccountMasked = receiverAccountMasked;
        this.sanitizedPayload = sanitizedPayload;
        this.status = status != null ? status : "UNMATCHED";
        this.resolvedBy = resolvedBy;
        this.resolutionNote = resolutionNote;
        this.resolvedAt = resolvedAt;
        this.receivedAt = receivedAt != null ? receivedAt : Instant.now();
    }

    public static UnmatchedBankInflow create(String provider, String providerTxId, long amount,
                                             String rawContent, String extractedReference,
                                             String mismatchReason, String matchedPaymentCode,
                                             String receiverAccountMasked, String sanitizedPayload) {
        return new UnmatchedBankInflow(UUID.randomUUID().toString(), provider, providerTxId, amount,
                rawContent, extractedReference, mismatchReason, matchedPaymentCode,
                receiverAccountMasked, sanitizedPayload, "UNMATCHED", null, null, null, Instant.now());
    }

    public void resolve(String paymentCode, String adminId, String note, String targetStatus) {
        this.matchedPaymentCode = paymentCode;
        this.resolvedBy = adminId;
        this.resolutionNote = note;
        this.status = targetStatus != null ? targetStatus : "RESOLVED_CONFIRMED";
        this.resolvedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getProvider() { return provider; }
    public String getProviderTxId() { return providerTxId; }
    public long getAmount() { return amount; }
    public String getRawContent() { return rawContent; }
    public String getExtractedReference() { return extractedReference; }
    public String getMismatchReason() { return mismatchReason; }
    public String getMatchedPaymentCode() { return matchedPaymentCode; }
    public String getReceiverAccountMasked() { return receiverAccountMasked; }
    public String getSanitizedPayload() { return sanitizedPayload; }
    public String getStatus() { return status; }
    public String getResolvedBy() { return resolvedBy; }
    public String getResolutionNote() { return resolutionNote; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getReceivedAt() { return receivedAt; }
}

