package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "payment_webhook_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentWebhookLogJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "payment_code", length = 64)
    private String paymentCode;

    @Column(name = "source_ip", length = 64)
    private String sourceIp;

    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid;

    @Column(name = "processing_status", nullable = false, length = 40)
    private String processingStatus;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "raw_payload", columnDefinition = "TEXT", nullable = false)
    private String rawPayload;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;
}

