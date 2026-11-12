package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "reconciliation_reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationReportJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "recon_date", nullable = false, unique = true)
    private LocalDate reconDate;

    @Column(name = "total_internal_tx", nullable = false)
    @Builder.Default
    private int totalInternalTx = 0;

    @Column(name = "total_gateway_tx", nullable = false)
    @Builder.Default
    private int totalGatewayTx = 0;

    @Column(name = "matched_tx", nullable = false)
    @Builder.Default
    private int matchedTx = 0;

    @Column(name = "mismatched_tx", nullable = false)
    @Builder.Default
    private int mismatchedTx = 0;

    @Column(name = "missing_internal_tx", nullable = false)
    @Builder.Default
    private int missingInternalTx = 0;

    @Column(name = "missing_provider_tx", nullable = false)
    @Builder.Default
    private int missingProviderTx = 0;

    @Column(length = 30, nullable = false)
    @Builder.Default
    private String status = "COMPLETED";

    @Column(name = "summary_notes", columnDefinition = "TEXT")
    private String summaryNotes;

    @Column(name = "sla_due_at")
    private Instant slaDueAt;

    @Column(name = "assigned_to", length = 36)
    private String assignedTo;

    @Column(name = "resolved_by", length = 36)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

