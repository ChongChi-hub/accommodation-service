package vn.cozynest.booking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "saga_instances")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaInstanceJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "saga_id", nullable = false, unique = true, length = 64)
    private String sagaId;

    @Column(name = "saga_type", nullable = false, length = 50)
    private String sagaType;

    @Column(name = "current_step", nullable = false, length = 50)
    private String currentStep;

    @Column(name = "payload_json", nullable = false, columnDefinition = "JSON")
    private String payloadJson;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "IN_PROGRESS";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

