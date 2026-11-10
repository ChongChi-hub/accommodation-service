package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "maker_checker_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MakerCheckerJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "maker_id", nullable = false, length = 36)
    private String makerId;

    @Column(name = "checker_id", length = 36)
    private String checkerId;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(nullable = false)
    @Builder.Default
    private long amount = 0L;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "payload_json", columnDefinition = "JSON")
    private String payloadJson;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

