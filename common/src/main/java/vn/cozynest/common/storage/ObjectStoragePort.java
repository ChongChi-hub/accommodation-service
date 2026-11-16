package vn.cozynest.common.storage;

import java.time.Instant;
import java.util.List;

/**
 * Unified Object Storage Port supporting Public CDN & Private Encrypted Buckets (STORAGE-01).
 * Interoperable with Cloudflare R2, AWS S3, MinIO, and Local encrypted storage.
 */
public interface ObjectStoragePort {

    String PUBLIC_BUCKET_DEFAULT = "cozynest-public";
    String PRIVATE_BUCKET_DEFAULT = "cozynest-private";

    StoredObjectMetadata putObject(String bucket, String key, byte[] content, String contentType, boolean isPrivate);

    byte[] getObject(String bucket, String key, boolean isPrivate);

    void deleteObject(String bucket, String key, boolean isPrivate);

    boolean exists(String bucket, String key, boolean isPrivate);

    List<StoredObjectMetadata> listObjects(String bucket, String prefix, boolean isPrivate);

    PresignedUploadResponse generatePresignedUploadUrl(
            String bucket, String key, String contentType, long maxSizeBytes, long ttlSeconds, boolean isPrivate);

    String generatePresignedDownloadUrl(String bucket, String key, long ttlSeconds, boolean isPrivate);

    record StoredObjectMetadata(
            String bucket,
            String objectKey,
            String sha256Hex,
            long sizeBytes,
            String contentType,
            boolean isPrivate,
            String publicUrl,
            Instant lastModified
    ) {
    }

    record PresignedUploadResponse(
            String uploadUrl,
            String objectKey,
            String bucket,
            String method,
            Instant expiresAt,
            String publicCdnUrl,
            boolean isPrivate
    ) {
    }
}
