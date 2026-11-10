package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "ledger_accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerAccountJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "account_number", nullable = false, unique = true, length = 50)
    private String accountNumber;

    @Column(name = "account_name", nullable = false, length = 150)
    private String accountName;

    @Column(name = "account_type", nullable = false, length = 30)
    private String accountType;

    @Column(name = "owner_id", length = 36)
    private String ownerId;

    @Column(nullable = false)
    @Builder.Default
    private long balance = 0L;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "is_frozen", nullable = false)
    @Builder.Default
    private boolean frozen = false;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private long version = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

