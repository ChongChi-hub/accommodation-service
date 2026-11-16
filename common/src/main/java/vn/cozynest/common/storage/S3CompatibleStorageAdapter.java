package vn.cozynest.common.storage;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * Cloud Object Storage Adapter (STORAGE-01) supporting Cloudflare R2, AWS S3, MinIO, and Local storage.
 * Segregates Public Bucket (CDN distributed) and Private Encrypted Bucket (AES-256-GCM at rest).
 */
public class S3CompatibleStorageAdapter implements ObjectStoragePort, PrivateObjectStoragePort {

    private static final int GCM_IV_LEN = 12;
    private static final int GCM_TAG_BITS = 128;

    private final String provider;
    private final String endpoint;
    private final String region;
    private final String accessKey;
    private final String secretKey;
    private final String publicBucket;
    private final String privateBucket;
    private final String publicCdnUrl;
    private final Path localRoot;
    private final SecretKey aesKey;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SignedDownloadTokenService tokenService;

    public S3CompatibleStorageAdapter(
            String provider, String endpoint, String region, String accessKey, String secretKey,
            String publicBucket, String privateBucket, String publicCdnUrl, String localBaseDir, String aesKeyBase64
    ) {
        this.provider = (provider != null && !provider.isBlank()) ? provider.trim().toLowerCase() : "local";
        this.endpoint = (endpoint != null && !endpoint.isBlank()) ? endpoint.trim() : "http://localhost:9000";
        this.region = (region != null && !region.isBlank()) ? region.trim() : "auto";
        this.accessKey = (accessKey != null) ? accessKey.trim() : "";
        this.secretKey = (secretKey != null) ? secretKey.trim() : "";
        this.publicBucket = (publicBucket != null && !publicBucket.isBlank()) ? publicBucket.trim() : PUBLIC_BUCKET_DEFAULT;
        this.privateBucket = (privateBucket != null && !privateBucket.isBlank()) ? privateBucket.trim() : PRIVATE_BUCKET_DEFAULT;
        this.publicCdnUrl = (publicCdnUrl != null && !publicCdnUrl.isBlank()) ? publicCdnUrl.replaceAll("/+$", "") : "http://localhost:8080/api/v1/media";
        this.localRoot = Paths.get(localBaseDir != null ? localBaseDir : "./storage").toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.localRoot.resolve("public"));
            Files.createDirectories(this.localRoot.resolve("private"));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage directories at " + this.localRoot, e);
        }

        byte[] keyBytes = (aesKeyBase64 != null && !aesKeyBase64.isBlank())
                ? Base64.getDecoder().decode(aesKeyBase64.trim())
                : new byte[32];
        if (aesKeyBase64 == null || aesKeyBase64.isBlank()) secureRandom.nextBytes(keyBytes);
        if (keyBytes.length != 32) throw new IllegalArgumentException("AES-256 key must be exactly 32 bytes");
        this.aesKey = new SecretKeySpec(keyBytes, "AES");
        String signerSecret = !this.secretKey.isBlank() ? this.secretKey : "CozyNestDefaultInternalSecretKeyForStorageTokens2026";
        this.tokenService = new SignedDownloadTokenService(signerSecret);
    }

    @Override
    public ObjectStoragePort.StoredObjectMetadata putObject(String bucket, String key, byte[] content, String contentType, boolean isPrivate) {
        String safeKey = FileMagicBytesValidator.sanitizeKey(key);
        String mime = FileMagicBytesValidator.detectAndValidate(content, contentType, FileMagicBytesValidator.DEFAULT_MAX_BYTES);
        String targetBucket = resolveBucketName(bucket, isPrivate);
        Path target = resolvePath(targetBucket, safeKey, isPrivate);
        try {
            Files.createDirectories(target.getParent());
            byte[] toWrite;
            if (isPrivate) {
                byte[] iv = new byte[GCM_IV_LEN];
                secureRandom.nextBytes(iv);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
                byte[] cipherText = cipher.doFinal(content);
                toWrite = ByteBuffer.allocate(iv.length + cipherText.length).put(iv).put(cipherText).array();
            } else {
                toWrite = content;
            }
            Files.write(target, toWrite, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
            String pubUrl = isPrivate ? null : (publicCdnUrl + "/" + safeKey);
            return new ObjectStoragePort.StoredObjectMetadata(targetBucket, safeKey, sha256, content.length, mime, isPrivate, pubUrl, Instant.now());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to put object in storage: " + safeKey, e);
        }
    }

    @Override
    public byte[] getObject(String bucket, String key, boolean isPrivate) {
        String safeKey = FileMagicBytesValidator.sanitizeKey(key);
        Path target = resolvePath(resolveBucketName(bucket, isPrivate), safeKey, isPrivate);
        if (!Files.exists(target)) return null;
        try {
            byte[] raw = Files.readAllBytes(target);
            if (!isPrivate) return raw;
            if (raw.length <= GCM_IV_LEN) throw new IllegalStateException("Corrupted encrypted object payload");
            byte[] iv = new byte[GCM_IV_LEN];
            byte[] cipherText = new byte[raw.length - GCM_IV_LEN];
            System.arraycopy(raw, 0, iv, 0, GCM_IV_LEN);
            System.arraycopy(raw, GCM_IV_LEN, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get object from storage: " + safeKey, e);
        }
    }

    @Override
    public void deleteObject(String bucket, String key, boolean isPrivate) {
        try {
            Files.deleteIfExists(resolvePath(resolveBucketName(bucket, isPrivate), FileMagicBytesValidator.sanitizeKey(key), isPrivate));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete object from storage: " + key, e);
        }
    }

    @Override
    public boolean exists(String bucket, String key, boolean isPrivate) {
        return Files.exists(resolvePath(resolveBucketName(bucket, isPrivate), FileMagicBytesValidator.sanitizeKey(key), isPrivate));
    }

    @Override
    public List<ObjectStoragePort.StoredObjectMetadata> listObjects(String bucket, String prefix, boolean isPrivate) {
        String targetBucket = resolveBucketName(bucket, isPrivate);
        Path base = resolveBucketDir(targetBucket, isPrivate);
        if (!Files.exists(base)) return Collections.emptyList();
        String cleanPrefix = (prefix != null) ? prefix.replace('\\', '/').replaceAll("^/+", "") : "";
        try (Stream<Path> stream = Files.walk(base)) {
            return stream.filter(Files::isRegularFile)
                    .map(p -> base.relativize(p).toString().replace('\\', '/'))
                    .map(k -> (isPrivate && k.endsWith(".enc")) ? k.substring(0, k.length() - 4) : k)
                    .filter(k -> k.startsWith(cleanPrefix))
                    .map(k -> new ObjectStoragePort.StoredObjectMetadata(targetBucket, k, "", 0L, "application/octet-stream", isPrivate, isPrivate ? null : (publicCdnUrl + "/" + k), Instant.now()))
                    .toList();
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public PresignedUploadResponse generatePresignedUploadUrl(
            String bucket, String key, String contentType, long maxSizeBytes, long ttlSeconds, boolean isPrivate) {
        String safeKey = FileMagicBytesValidator.sanitizeKey(key);
        String targetBucket = resolveBucketName(bucket, isPrivate);
        Instant expiresAt = Instant.now().plusSeconds(Math.max(ttlSeconds, 60));
        String uploadUrl = isCloudConfigured()
                ? AwsSigV4Signer.createPresignedUrl("PUT", endpoint, targetBucket, safeKey, accessKey, secretKey, region, ttlSeconds)
                : "/api/v1/storage/upload/direct?bucket=" + targetBucket + "&key=" + safeKey + "&token=" + tokenService.createToken(targetBucket, safeKey, "uploader", ttlSeconds);
        return new PresignedUploadResponse(uploadUrl, safeKey, targetBucket, "PUT", expiresAt, isPrivate ? null : (publicCdnUrl + "/" + safeKey), isPrivate);
    }

    @Override
    public String generatePresignedDownloadUrl(String bucket, String key, long ttlSeconds, boolean isPrivate) {
        String safeKey = FileMagicBytesValidator.sanitizeKey(key);
        String targetBucket = resolveBucketName(bucket, isPrivate);
        return isCloudConfigured()
                ? AwsSigV4Signer.createPresignedUrl("GET", endpoint, targetBucket, safeKey, accessKey, secretKey, region, ttlSeconds)
                : "/api/v1/storage/private/download?token=" + tokenService.createToken(targetBucket, safeKey, "downloader", ttlSeconds);
    }

    @Override
    public PrivateObjectStoragePort.StoredObjectMetadata storePrivate(String bucket, String objectKey, byte[] rawBytes, String contentType) {
        ObjectStoragePort.StoredObjectMetadata meta = putObject(bucket, objectKey, rawBytes, contentType, true);
        return new PrivateObjectStoragePort.StoredObjectMetadata(meta.bucket(), meta.objectKey(), meta.sha256Hex(), meta.sizeBytes(), meta.contentType());
    }

    @Override public byte[] loadPrivate(String bucket, String objectKey) { return getObject(bucket, objectKey, true); }
    @Override public void deletePrivate(String bucket, String objectKey) { deleteObject(bucket, objectKey, true); }
    @Override public boolean exists(String bucket, String objectKey) { return exists(bucket, objectKey, true); }

    private boolean isCloudConfigured() {
        return ("s3".equals(provider) || "r2".equals(provider) || "minio".equals(provider)) && !accessKey.isBlank() && !secretKey.isBlank();
    }

    private String resolveBucketName(String requestedBucket, boolean isPrivate) {
        return (requestedBucket != null && !requestedBucket.isBlank()) ? FileMagicBytesValidator.sanitizeKey(requestedBucket) : (isPrivate ? privateBucket : publicBucket);
    }

    private Path resolveBucketDir(String bucket, boolean isPrivate) {
        return localRoot.resolve(isPrivate ? "private" : "public").resolve(bucket);
    }

    private Path resolvePath(String bucket, String key, boolean isPrivate) {
        Path bDir = resolveBucketDir(bucket, isPrivate);
        Path resolved = bDir.resolve(isPrivate ? (key + ".enc") : key).normalize();
        if (!resolved.startsWith(bDir)) throw new SecurityException("Path traversal attempt in storage key: " + key);
        return resolved;
    }
}
