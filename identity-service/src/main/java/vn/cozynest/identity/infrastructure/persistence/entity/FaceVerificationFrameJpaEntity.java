package vn.cozynest.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "face_verification_frames")
@Getter
@Setter
@NoArgsConstructor
public class FaceVerificationFrameJpaEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "face_verification_id", nullable = false, length = 36)
    private String faceVerificationId;

    @Column(name = "purpose", nullable = false, length = 50)
    private String purpose;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    @Column(name = "size", nullable = false)
    private long size;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}

