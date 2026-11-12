package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "risk_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskProfileJpaEntity {
    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "daily_limit", nullable = false)
    @Builder.Default
    private long dailyLimit = 20_000_000L;

    @Column(name = "daily_withdrawn_amount", nullable = false)
    @Builder.Default
    private long dailyWithdrawnAmount = 0L;

    @Column(name = "last_withdrawal_date")
    private LocalDate lastWithdrawalDate;

    @Column(name = "velocity_count", nullable = false)
    @Builder.Default
    private int velocityCount = 0;

    @Column(name = "is_frozen", nullable = false)
    @Builder.Default
    private boolean frozen = false;

    @Column(name = "freeze_reason", length = 255)
    private String freezeReason;

    @Column(name = "bank_changed_at")
    private Instant bankChangedAt;

    @Column(name = "is_blacklisted", nullable = false)
    @Builder.Default
    private boolean blacklisted = false;

    @Column(name = "blacklist_reason", length = 255)
    private String blacklistReason;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

