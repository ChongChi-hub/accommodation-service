package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "bank_transaction_registry")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTransactionRecordJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "normalized_tx_code", nullable = false, unique = true, length = 64)
    private String normalizedTxCode;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "direction", nullable = false, length = 10)
    private String direction;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "reference_code", length = 64)
    private String referenceCode;

    @Column(name = "order_type", length = 40)
    private String orderType;

    @Column(name = "order_id", length = 64)
    private String orderId;

    @Column(name = "receiver_account_masked", length = 32)
    private String receiverAccountMasked;

    @Column(name = "raw_content", length = 500)
    private String rawContent;

    @Column(name = "sanitized_payload", columnDefinition = "TEXT")
    private String sanitizedPayload;

    @Column(name = "recorded_by", nullable = false, length = 36)
    private String recordedBy;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

