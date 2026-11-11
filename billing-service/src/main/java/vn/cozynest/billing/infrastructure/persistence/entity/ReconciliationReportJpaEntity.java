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

    @Column(length = 30, nullable = false)
    @Builder.Default
    private String status = "COMPLETED";

    @Column(name = "summary_notes", columnDefinition = "TEXT")
    private String summaryNotes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

