package vn.cozynest.booking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "contract_occupants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractOccupantJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "contract_id", nullable = false, length = 36)
    private String contractId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "id_card_number", nullable = false, length = 30)
    private String idCardNumber;

    @Column(length = 50)
    private String relationship;

    @Column(name = "is_primary_tenant", nullable = false)
    @Builder.Default
    private boolean primaryTenant = false;

    @Column(name = "removed_at")
    private Instant removedAt;

    @Column(name = "removed_by", length = 36)
    private String removedBy;

    @Column(name = "removal_reason", length = 255)
    private String removalReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

