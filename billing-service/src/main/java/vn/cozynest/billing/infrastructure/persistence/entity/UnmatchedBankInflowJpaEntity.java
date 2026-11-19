package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "unmatched_bank_inflows")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnmatchedBankInflowJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_tx_id", nullable = false, unique = true, length = 64)
    private String providerTxId;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "raw_content", length = 500)
    private String rawContent;

    @Column(name = "extracted_reference", length = 64)
    private String extractedReference;

    @Column(name = "mismatch_reason", nullable = false, length = 100)
    private String mismatchReason;

    @Column(name = "matched_payment_code", length = 64)
    private String matchedPaymentCode;

    @Column(name = "receiver_account_masked", length = 32)
    private String receiverAccountMasked;

    @Column(name = "sanitized_payload", columnDefinition = "TEXT")
    private String sanitizedPayload;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "resolved_by", length = 36)
    private String resolvedBy;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}

