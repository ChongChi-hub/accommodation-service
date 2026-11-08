package vn.cozynest.identity.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class KycCryptoService {

    private static final String AES_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String HMAC_ALGO = "HmacSHA256";

    private final byte[] aesKeyBytes;
    private final byte[] pepperBytes;
    private final SecureRandom secureRandom = new SecureRandom();

    public KycCryptoService(String secretKey, String pepper) {
        this(secretKey, pepper, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public KycCryptoService(
            @Value("${app.kyc.secret-key:CozyNestKycSecretKey2026DevEnvironment32B}") String secretKey,
            @Value("${app.kyc.pepper:CozyNestKycPepperSecret2026DevEnvKey!}") String pepper,
            @org.springframework.beans.factory.annotation.Autowired(required = false) org.springframework.core.env.Environment env) {
        if (env != null && env.matchesProfiles("prod", "production")) {
            String envSecret = System.getenv("KYC_SECRET_KEY");
            String envPepper = System.getenv("KYC_PEPPER");
            if (envSecret == null || envSecret.isBlank() || envPepper == null || envPepper.isBlank()
                    || "CozyNestKycSecretKey2026DevEnvironment32B".equals(secretKey)) {
                throw new IllegalStateException("Production environment must explicitly configure KYC_SECRET_KEY and KYC_PEPPER via environment variables");
            }
        }
        byte[] keyRaw = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyRaw.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyRaw, 0, padded, 0, keyRaw.length);
            this.aesKeyBytes = padded;
        } else if (keyRaw.length > 32) {
            byte[] truncated = new byte[32];
            System.arraycopy(keyRaw, 0, truncated, 0, 32);
            this.aesKeyBytes = truncated;
        } else {
            this.aesKeyBytes = keyRaw;
        }
        this.pepperBytes = pepper.getBytes(StandardCharsets.UTF_8);
    }

    public String encryptIdNumber(String plainIdNumber) {
        if (plainIdNumber == null || plainIdNumber.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(aesKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(plainIdNumber.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Loi ma hoa so can cuoc cong dan", e);
        }
    }

    public String decryptIdNumber(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);
            if (decoded.length < GCM_IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Du lieu ma hoa khong hop le");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(aesKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Loi giai ma so can cuoc cong dan", e);
        }
    }

    public String hashIdNumber(String plainIdNumber) {
        if (plainIdNumber == null || plainIdNumber.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKey = new SecretKeySpec(pepperBytes, HMAC_ALGO);
            mac.init(secretKey);
            byte[] hashBytes = mac.doFinal(plainIdNumber.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Loi tinh bam so can cuoc cong dan", e);
        }
    }

    public String extractLast4(String plainIdNumber) {
        if (plainIdNumber == null || plainIdNumber.length() < 4) {
            return plainIdNumber;
        }
        return plainIdNumber.substring(plainIdNumber.length() - 4);
    }

    public String maskIdNumber(String last4) {
        if (last4 == null || last4.isBlank()) {
            return "Chua cap nhat";
        }
        return "********" + last4;
    }
}

