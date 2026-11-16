package vn.cozynest.listing.domain.model;

import java.time.Instant;
import java.util.UUID;

public class MediaUpload {

    private final String id;
    private final String objectKey;
    private final String bucket;
    private final String category;
    private String contentType;
    private long sizeBytes;
    private String status;
    private final String uploaderId;
    private String attachedEntityType;
    private String attachedEntityId;
    private final Instant createdAt;
    private Instant confirmedAt;
    private Instant attachedAt;

    public MediaUpload(
            String id, String objectKey, String bucket, String category, String contentType,
            long sizeBytes, String status, String uploaderId, String attachedEntityType,
            String attachedEntityId, Instant createdAt, Instant confirmedAt, Instant attachedAt
    ) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.objectKey = objectKey;
        this.bucket = bucket;
        this.category = category;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.status = status != null ? status : "PENDING_UPLOAD";
        this.uploaderId = uploaderId;
        this.attachedEntityType = attachedEntityType;
        this.attachedEntityId = attachedEntityId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.confirmedAt = confirmedAt;
        this.attachedAt = attachedAt;
    }

    public static MediaUpload createPending(String objectKey, String bucket, String category, String contentType, long sizeBytes, String uploaderId) {
        return new MediaUpload(
                UUID.randomUUID().toString(), objectKey, bucket, category, contentType,
                sizeBytes, "PENDING_UPLOAD", uploaderId, null, null, Instant.now(), null, null
        );
    }

    public void confirm(long finalSizeBytes, String finalMime) {
        this.status = "CONFIRMED";
        this.sizeBytes = finalSizeBytes;
        if (finalMime != null && !finalMime.isBlank()) {
            this.contentType = finalMime;
        }
        this.confirmedAt = Instant.now();
    }

    public void attach(String entityType, String entityId) {
        this.attachedEntityType = entityType;
        this.attachedEntityId = entityId;
        this.status = "ATTACHED";
        this.attachedAt = Instant.now();
    }

    public void markOrphanedDeleted() {
        this.status = "ORPHANED_DELETED";
    }

    public String getId() { return id; }
    public String getObjectKey() { return objectKey; }
    public String getBucket() { return bucket; }
    public String getCategory() { return category; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStatus() { return status; }
    public String getUploaderId() { return uploaderId; }
    public String getAttachedEntityType() { return attachedEntityType; }
    public String getAttachedEntityId() { return attachedEntityId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getAttachedAt() { return attachedAt; }
}
