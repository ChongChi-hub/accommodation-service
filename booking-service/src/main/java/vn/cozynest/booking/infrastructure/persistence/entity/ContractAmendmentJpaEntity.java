package vn.cozynest.booking.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "contract_amendments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractAmendmentJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "contract_id", nullable = false, length = 36)
    private String contractId;

    @Column(name = "amendment_code", nullable = false, unique = true, length = 64)
    private String amendmentCode;

    @Column(name = "amendment_type", nullable = false, length = 50)
    private String amendmentType;

    @Column(name = "new_rent")
    private Long newRent;

    @Column(name = "new_end_date")
    private LocalDate newEndDate;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING_SIGN";

    @Column(name = "tenant_signed_at")
    private Instant tenantSignedAt;

    @Column(name = "landlord_signed_at")
    private Instant landlordSignedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

