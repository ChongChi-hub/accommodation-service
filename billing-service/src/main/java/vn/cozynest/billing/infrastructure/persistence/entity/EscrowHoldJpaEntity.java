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

    @Column(name = "inspection_ends_at", nullable = false)
    private Instant inspectionEndsAt;

    @Column(name = "dispute_reason", length = 500)
    private String disputeReason;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

