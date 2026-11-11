package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "invoice_code", nullable = false, unique = true, length = 50)
    private String invoiceCode;

    @Column(name = "contract_id", nullable = false, length = 36)
    private String contractId;

    @Column(name = "utility_reading_id", length = 36)
    private String utilityReadingId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "landlord_id", nullable = false, length = 36)
    private String landlordId;

    @Column(name = "period_month", nullable = false)
    private LocalDate periodMonth;

    @Column(name = "base_rent", nullable = false)
    private long baseRent;

    @Column(name = "electricity_amount", nullable = false)
    @Builder.Default
    private long electricityAmount = 0L;

    @Column(name = "water_amount", nullable = false)
    @Builder.Default
    private long waterAmount = 0L;

    @Column(name = "service_amount", nullable = false)
    @Builder.Default
    private long serviceAmount = 0L;

    @Column(name = "other_amount", nullable = false)
    @Builder.Default
    private long otherAmount = 0L;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "paid_amount", nullable = false)
    @Builder.Default
    private long paidAmount = 0L;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ISSUED";

    @Column(name = "invoice_type", nullable = false, length = 30)
    @Builder.Default
    private String invoiceType = "STANDARD";

    @Column(name = "reference_invoice_id", length = 36)
    private String referenceInvoiceId;

    @Column(name = "credit_note_reason", length = 255)
    private String creditNoteReason;

    @Column(name = "overdue_notified_at")
    private Instant overdueNotifiedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private long version = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

