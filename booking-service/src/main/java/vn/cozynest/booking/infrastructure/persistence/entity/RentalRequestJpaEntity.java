package vn.cozynest.booking.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "rental_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalRequestJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "room_id", nullable = false, length = 64)
    private String roomId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "landlord_id", nullable = false, length = 36)
    private String landlordId;

    @Column(name = "proposed_move_in_date", nullable = false)
    private LocalDate proposedMoveInDate;

    @Column(name = "num_occupants", nullable = false)
    @Builder.Default
    private int numOccupants = 1;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "contract_id", length = 36)
    private String contractId;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "holding_expires_at")
    private Instant holdingExpiresAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private long version = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

