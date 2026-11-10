package vn.cozynest.identity.infrastructure.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;

@Component
public class TotpHelper {
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int DIGIT_MODULO = 1_000_000;

    public String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    public boolean verifyCode(String base32Secret, String inputCode) {
        if (base32Secret == null || inputCode == null) return false;
        long currentWindow = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        // Check window -1, 0, +1 for clock drift
        for (int i = -1; i <= 1; i++) {
            String expected = generateCode(base32Secret, currentWindow + i);
            if (expected.equals(inputCode.trim())) {
                return true;
            }
        }
        return false;
    }

    public String generateCode(String base32Secret, long timeWindow) {
        byte[] key = decodeBase32(base32Secret);
        byte[] data = ByteBuffer.allocate(8).putLong(timeWindow).array();
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "RAW"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            long truncated = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            long code = truncated % DIGIT_MODULO;
            return String.format("%06d", code);
        } catch (Exception e) {
            throw new RuntimeException("Error computing TOTP code", e);
        }
    }

    public String buildOtpAuthUri(String secret, String accountName, String issuer) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                issuer, accountName, secret, issuer, CODE_DIGITS, TIME_STEP_SECONDS);
    }

    private static String encodeBase32(byte[] data) {
        StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                sb.append(BASE32_CHARS.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return sb.toString();
    }

    private static byte[] decodeBase32(String base32) {
        String clean = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int buffer = 0, bitsLeft = 0, count = 0;
        byte[] result = new byte[clean.length() * 5 / 8];
        for (int i = 0; i < clean.length(); i++) {
            int val = BASE32_CHARS.indexOf(clean.charAt(i));
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                result[count++] = (byte) ((buffer >> bitsLeft) & 0xFF);
            }
        }
        byte[] exact = new byte[count];
        System.arraycopy(result, 0, exact, 0, count);
        return exact;
    }
}

