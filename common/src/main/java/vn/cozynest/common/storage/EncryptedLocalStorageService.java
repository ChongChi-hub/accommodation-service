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
import java.util.Base64;
import java.util.HexFormat;

/**
 * Local private filesystem object store encrypting every stored object at rest with AES-256-GCM.
 */
public class EncryptedLocalStorageService implements PrivateObjectStoragePort {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private final Path rootDir;
    private final SecretKey aesKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptedLocalStorageService(String baseDir, String aesKeyBase64, boolean prodMode) {
        this.rootDir = Paths.get(baseDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot initialize private storage directory", e);
        }

        byte[] keyBytes;
        if (aesKeyBase64 != null && !aesKeyBase64.isBlank()) {
            keyBytes = Base64.getDecoder().decode(aesKeyBase64.trim());
        } else {
            if (prodMode) {
                throw new IllegalStateException("Production profile requires STORAGE_AES_KEY_BASE64 (32 bytes)");
            }
            keyBytes = new byte[32];
            secureRandom.nextBytes(keyBytes);
        }
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("AES-256 key must be exactly 32 bytes");
        }
        this.aesKey = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public StoredObjectMetadata storePrivate(String bucket, String objectKey, byte[] rawBytes, String contentType) {
        String verifiedMime = FileMagicBytesValidator.detectAndValidate(rawBytes, contentType, FileMagicBytesValidator.DEFAULT_MAX_BYTES);
        Path target = resolveTarget(bucket, objectKey);
        try {
            Files.createDirectories(target.getParent());
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(rawBytes);

            byte[] packed = ByteBuffer.allocate(iv.length + cipherText.length).put(iv).put(cipherText).array();
            Files.write(target, packed, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rawBytes));
            return new StoredObjectMetadata(
                    FileMagicBytesValidator.sanitizeKey(bucket),
                    FileMagicBytesValidator.sanitizeKey(objectKey),
                    sha256,
                    rawBytes.length,
                    verifiedMime
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to store encrypted private object", e);
        }
    }

    @Override
    public byte[] loadPrivate(String bucket, String objectKey) {
        Path target = resolveTarget(bucket, objectKey);
        if (!Files.exists(target)) {
            return null;
        }
        try {
            byte[] packed = Files.readAllBytes(target);
            if (packed.length <= GCM_IV_LENGTH) {
                throw new IllegalStateException("Corrupted encrypted object payload");
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[packed.length - GCM_IV_LENGTH];
            System.arraycopy(packed, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(packed, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt private object", e);
        }
    }

    @Override
    public void deletePrivate(String bucket, String objectKey) {
        try {
            Files.deleteIfExists(resolveTarget(bucket, objectKey));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete private object", e);
        }
    }

    @Override
    public boolean exists(String bucket, String objectKey) {
        return Files.exists(resolveTarget(bucket, objectKey));
    }

    private Path resolveTarget(String bucket, String objectKey) {
        String safeBucket = FileMagicBytesValidator.sanitizeKey(bucket);
        String safeKey = FileMagicBytesValidator.sanitizeKey(objectKey);
        Path resolved = rootDir.resolve(safeBucket).resolve(safeKey + ".enc").normalize();
        if (!resolved.startsWith(rootDir)) {
            throw new SecurityException("Path traversal detected");
        }
        return resolved;
    }
}

