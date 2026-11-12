package vn.cozynest.booking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "contract_code", nullable = false, unique = true, length = 50)
    private String contractCode;

    @Column(name = "room_id", nullable = false, length = 64)
    private String roomId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "landlord_id", nullable = false, length = 36)
    private String landlordId;

    @Column(name = "request_id", length = 36)
    private String requestId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "monthly_rent", nullable = false)
    private long monthlyRent;

    @Column(name = "deposit_amount", nullable = false)
    @Builder.Default
    private long depositAmount = 0L;

    @Column(name = "payment_cycle_months", nullable = false)
    @Builder.Default
    private int paymentCycleMonths = 1;

    @Column(name = "payment_due_day", nullable = false)
    @Builder.Default
    private int paymentDueDay = 5;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String terms;

    @Column(name = "document_hash", length = 64)
    private String documentHash;

    @Column(name = "verification_code", length = 64)
    private String verificationCode;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "pdf_object_key", length = 255)
    private String pdfObjectKey;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING_SIGN";

    @Column(name = "expiry_notified_days", length = 64)
    private String expiryNotifiedDays;

    @Column(name = "holding_expires_at")
    private Instant holdingExpiresAt;

    @Column(name = "tenant_signed_at")
    private Instant tenantSignedAt;

    @Column(name = "landlord_signed_at")
    private Instant landlordSignedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private long version = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

