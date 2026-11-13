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

    @Column(name = "initial_electricity_reading", precision = 10, scale = 2)
    @Builder.Default
    private java.math.BigDecimal initialElectricityReading = java.math.BigDecimal.ZERO;

    @Column(name = "initial_water_reading", precision = 10, scale = 2)
    @Builder.Default
    private java.math.BigDecimal initialWaterReading = java.math.BigDecimal.ZERO;

    @Column(name = "electricity_rate", precision = 10, scale = 2)
    @Builder.Default
    private java.math.BigDecimal electricityRate = java.math.BigDecimal.valueOf(3500);

    @Column(name = "water_rate", precision = 10, scale = 2)
    @Builder.Default
    private java.math.BigDecimal waterRate = java.math.BigDecimal.valueOf(25000);

    @Column(name = "physical_contract_status", length = 32)
    @Builder.Default
    private String physicalContractStatus = "PENDING_PHYSICAL_SIGN";

    @Column(name = "physical_contract_scan_url", length = 500)
    private String physicalContractScanUrl;

    @Column(name = "physical_signed_at")
    private Instant physicalSignedAt;

    @Column(name = "tenant_confirmed_at")
    private Instant tenantConfirmedAt;

    @Column(name = "tenant_reject_reason", length = 500)
    private String tenantRejectReason;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private long version = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

