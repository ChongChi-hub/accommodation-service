package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "escrow_holds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscrowHoldJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "contract_id", nullable = false, unique = true, length = 64)
    private String contractId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "landlord_id", nullable = false, length = 36)
    private String landlordId;

    @Column(nullable = false)
    private long amount;

    @Column(name = "released_amount", nullable = false)
    private long releasedAmount;

    @Column(name = "retained_amount", nullable = false)
    private long retainedAmount;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "sandbox_mode", nullable = false)
    private boolean sandboxMode;

    @Column(name = "tenant_confirmed_move_in", nullable = false)
    private boolean tenantConfirmedMoveIn;

    @Column(name = "landlord_confirmed_move_in", nullable = false)
    private boolean landlordConfirmedMoveIn;

    @Column(name = "move_in_confirmed_at")
    private Instant moveInConfirmedAt;

    @Column(name = "pending_release_until")
    private Instant pendingReleaseUntil;

    @Column(name = "pending_settled", nullable = false)
    private boolean pendingSettled;

    @Column(name = "inspection_ends_at", nullable = false)
    private Instant inspectionEndsAt;

    @Column(name = "dispute_reason", length = 500)
    private String disputeReason;

    @Column(name = "payment_code", length = 64)
    private String paymentCode;

    @Column(name = "bank_tx_code", length = 64)
    private String bankTxCode;

    @Column(name = "receipt_hash", length = 128)
    private String receiptHash;

    @Column(name = "receipt_url", columnDefinition = "MEDIUMTEXT")
    private String receiptUrl;

    @Column(name = "deduction_claim_amount")
    private Long deductionClaimAmount;

    @Column(name = "deduction_claim_reason", length = 500)
    private String deductionClaimReason;

    @Column(name = "deduction_claim_proof_url", length = 500)
    private String deductionClaimProofUrl;

    @Column(name = "deduction_claim_status", length = 30)
    private String deductionClaimStatus;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
