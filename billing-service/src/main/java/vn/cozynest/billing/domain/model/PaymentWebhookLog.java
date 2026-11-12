package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.util.UUID;

public class PaymentWebhookLog {
    private final String id;
    private final String paymentCode;
    private final String sourceIp;
    private final boolean signatureValid;
    private final String processingStatus;
    private final String errorMessage;
    private final String rawPayload;
    private final Instant receivedAt;

    public PaymentWebhookLog(String id, String paymentCode, String sourceIp, boolean signatureValid,
                             String processingStatus, String errorMessage, String rawPayload, Instant receivedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.paymentCode = paymentCode;
        this.sourceIp = sourceIp;
        this.signatureValid = signatureValid;
        this.processingStatus = processingStatus;
        this.errorMessage = errorMessage;
        this.rawPayload = rawPayload != null ? rawPayload : "";
        this.receivedAt = receivedAt != null ? receivedAt : Instant.now();
    }

    public String getId() { return id; }
    public String getPaymentCode() { return paymentCode; }
    public String getSourceIp() { return sourceIp; }
    public boolean isSignatureValid() { return signatureValid; }
    public String getProcessingStatus() { return processingStatus; }
    public String getErrorMessage() { return errorMessage; }
    public String getRawPayload() { return rawPayload; }
    public Instant getReceivedAt() { return receivedAt; }
}

