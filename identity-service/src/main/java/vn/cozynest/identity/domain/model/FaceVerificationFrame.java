package vn.cozynest.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class FaceVerificationFrame {

    private final String id;
    private final String faceVerificationId;
    private final String purpose;
    private final String storageKey;
    private final String checksum;
    private final long size;
    private final Instant createdAt;
    private Instant deletedAt;

    public FaceVerificationFrame(
            String id, String faceVerificationId, String purpose,
            String storageKey, String checksum, long size,
            Instant createdAt, Instant deletedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.faceVerificationId = faceVerificationId;
        this.purpose = purpose;
        this.storageKey = storageKey;
        this.checksum = checksum;
        this.size = size;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.deletedAt = deletedAt;
    }

    public static FaceVerificationFrame create(
            String faceVerificationId, String purpose, String storageKey, String checksum, long size) {
        return new FaceVerificationFrame(
                UUID.randomUUID().toString(), faceVerificationId, purpose,
                storageKey, checksum, size, Instant.now(), null
        );
    }

    public void markDeleted() {
        this.deletedAt = Instant.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public String getId() { return id; }
    public String getFaceVerificationId() { return faceVerificationId; }
    public String getPurpose() { return purpose; }
    public String getStorageKey() { return storageKey; }
    public String getChecksum() { return checksum; }
    public long getSize() { return size; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeletedAt() { return deletedAt; }
}

