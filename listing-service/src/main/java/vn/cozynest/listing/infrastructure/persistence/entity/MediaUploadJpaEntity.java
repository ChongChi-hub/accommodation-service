package vn.cozynest.listing.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "media_uploads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaUploadJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "object_key", nullable = false, length = 255, unique = true)
    private String objectKey;

    @Column(nullable = false, length = 100)
    private String bucket;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "uploader_id", nullable = false, length = 64)
    private String uploaderId;

    @Column(name = "attached_entity_type", length = 50)
    private String attachedEntityType;

    @Column(name = "attached_entity_id", length = 64)
    private String attachedEntityId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "attached_at")
    private Instant attachedAt;
}
