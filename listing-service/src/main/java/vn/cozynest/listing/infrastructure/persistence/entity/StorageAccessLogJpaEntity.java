package vn.cozynest.listing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "storage_access_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageAccessLogJpaEntity {

    @Id
    @Column(length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(name = "actor_id", nullable = false, length = 64)
    private String actorId;

    @Column(name = "actor_role", nullable = false, length = 40)
    private String actorRole;

    @Column(nullable = false, length = 60)
    private String bucket;

    @Column(name = "object_key", nullable = false, length = 255)
    private String objectKey;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(nullable = false)
    private boolean allowed;

    @Column(length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

