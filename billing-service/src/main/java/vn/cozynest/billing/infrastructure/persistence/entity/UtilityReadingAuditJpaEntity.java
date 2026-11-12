package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "utility_reading_audits")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilityReadingAuditJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "reading_id", nullable = false, length = 36)
    private String readingId;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(name = "prev_snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String prevSnapshotJson;

    @Column(name = "actor_id", nullable = false, length = 36)
    private String actorId;

    @Column(length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

