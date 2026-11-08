package vn.cozynest.identity.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import vn.cozynest.identity.domain.exception.IdentityDomainException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class FaceCryptoService {

    private static final Logger log = LoggerFactory.getLogger(FaceCryptoService.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    // Development fallback key (32 bytes = 256 bits): CozyNestFaceDevEncryptionKey2026
    private static final String DEV_FALLBACK_KEY_BASE64 = "Q296eU5lc3RGYWNlRGV2RW5jcnlwdGlvbktleTIwMjY=";

    private final SecretKey encryptionKey;

    @Autowired
    public FaceCryptoService(
            @Value("${face.encryption-key:}") String configuredKeyBase64,
            @Autowired(required = false) Environment env) {
        boolean isProd = env != null && (env.matchesProfiles("prod", "production")
                || "production".equalsIgnoreCase(System.getenv("APP_ENV")));

        if (isProd) {
            String envKey = System.getenv("FACE_ENCRYPTION_KEY");
            String activeKey = (envKey != null && !envKey.isBlank()) ? envKey : configuredKeyBase64;
            if (activeKey == null || activeKey.isBlank() || activeKey.contains("REPLACE_WITH")) {
                throw new IllegalStateException("Production environment FATAL: FACE_ENCRYPTION_KEY is required and must not be placeholder!");
            }
        }

        String rawKey = (configuredKeyBase64 != null && !configuredKeyBase64.isBlank() && !configuredKeyBase64.contains("REPLACE_WITH"))
                ? configuredKeyBase64
                : DEV_FALLBACK_KEY_BASE64;

        byte[] decodedKey = Base64.getDecoder().decode(rawKey);
        if (decodedKey.length != 32) {
            throw new IllegalArgumentException("FACE_ENCRYPTION_KEY must be exactly 32 bytes (256-bit AES key) when base64-decoded");
        }
        this.encryptionKey = new SecretKeySpec(decodedKey, "AES");
    }

    public byte[] encrypt(byte[] plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, spec);

            byte[] ciphertext = cipher.doFinal(plaintext);

            // Output: [12-byte IV][Ciphertext + 16-byte Tag]
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return buffer.array();
        } catch (Exception e) {
            throw new IdentityDomainException("Lỗi mã hóa dữ liệu sinh trắc học khuôn mặt", "ERR_FACE_CRYPTO_FAILURE");
        }
    }

    public byte[] decrypt(byte[] payload) {
        try {
            if (payload == null || payload.length < IV_LENGTH_BYTES + 16) {
                throw new IllegalArgumentException("Ciphertext payload too short for AES-GCM");
            }
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, spec);

            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new IdentityDomainException("Lỗi giải mã dữ liệu sinh trắc học khuôn mặt", "ERR_FACE_CRYPTO_FAILURE");
        }
    }

    public String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}

