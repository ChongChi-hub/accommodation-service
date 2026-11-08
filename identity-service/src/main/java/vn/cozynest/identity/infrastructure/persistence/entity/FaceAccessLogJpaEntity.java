package vn.cozynest.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "face_access_logs")
@Getter
@Setter
@NoArgsConstructor
public class FaceAccessLogJpaEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "actor_id", nullable = false, length = 36)
    private String actorId;

    @Column(name = "actor_role", nullable = false, length = 50)
    private String actorRole;

    @Column(name = "frame_id", length = 36)
    private String frameId;

    @Column(name = "face_verification_id", nullable = false, length = 36)
    private String faceVerificationId;

    @Column(name = "purpose", nullable = false, length = 100)
    private String purpose;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

