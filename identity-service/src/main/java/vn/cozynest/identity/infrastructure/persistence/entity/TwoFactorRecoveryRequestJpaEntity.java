package vn.cozynest.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "two_factor_recovery_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorRecoveryRequestJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "target_user_id", nullable = false, length = 36)
    private String targetUserId;

    @Column(name = "requested_by", nullable = false, length = 36)
    private String requestedBy;

    @Column(name = "approved_by", length = 36)
    private String approvedBy;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}

