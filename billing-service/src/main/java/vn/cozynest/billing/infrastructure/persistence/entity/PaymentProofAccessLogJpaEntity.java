package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "payment_proof_access_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProofAccessLogJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "proof_id", nullable = false, length = 36)
    private String proofId;

    @Column(name = "payment_code", nullable = false, length = 64)
    private String paymentCode;

    @Column(name = "actor_id", nullable = false, length = 36)
    private String actorId;

    @Column(name = "actor_role", nullable = false, length = 40)
    private String actorRole;

    @Column(name = "action", nullable = false, length = 40)
    private String action;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

