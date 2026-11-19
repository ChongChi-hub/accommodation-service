package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "payment_proof_registry")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProofRecordJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "payment_code", nullable = false, length = 64)
    private String paymentCode;

    @Column(name = "order_type", nullable = false, length = 40)
    private String orderType;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "uploader_id", nullable = false, length = 36)
    private String uploaderId;

    @Column(name = "normalized_bank_tx_code", nullable = false, unique = true, length = 64)
    private String normalizedBankTxCode;

    @Column(name = "sha256_hash", nullable = false, unique = true, length = 128)
    private String sha256Hash;

    @Column(name = "dhash", nullable = false, length = 32)
    private String dhash;

    @Column(name = "near_duplicate_flag", nullable = false)
    private boolean nearDuplicateFlag;

    @Column(name = "near_duplicate_of_id", length = 36)
    private String nearDuplicateOfId;

    @Column(name = "mime_type", nullable = false, length = 40)
    private String mimeType;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

