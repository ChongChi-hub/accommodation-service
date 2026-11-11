package vn.cozynest.common.storage;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

/**
 * Generates and verifies short-lived HMAC-SHA256 signed download tokens bound to bucket, key, and actor/owner.
 */
public class SignedDownloadTokenService {

    private static final long MAX_TTL_SECONDS = 600;
    private final byte[] hmacSecretBytes;
    private final Clock clock;

    public SignedDownloadTokenService(String secret) {
        this(secret, Clock.systemUTC());
    }

    public SignedDownloadTokenService(String secret, Clock clock) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Signed URL secret must not be blank");
        }
        this.hmacSecretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    public String createToken(String bucket, String objectKey, String authorizedActorId, long ttlSeconds) {
        long clampedTtl = Math.min(Math.max(ttlSeconds, 10), MAX_TTL_SECONDS);
        long expiresEpoch = clock.instant().plusSeconds(clampedTtl).getEpochSecond();
        String safeBucket = FileMagicBytesValidator.sanitizeKey(bucket);
        String safeKey = FileMagicBytesValidator.sanitizeKey(objectKey);
        String actor = authorizedActorId == null ? "" : authorizedActorId;
        String payload = safeBucket + "|" + safeKey + "|" + actor + "|" + expiresEpoch;
        String sig = signBase64Url(payload);
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + sig;
    }

    public VerifiedToken verify(String token) {
        if (token == null || !token.contains(".")) {
            throw new SecurityException("Invalid signed token format");
        }
        String[] parts = token.split("\\.", 2);
        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String expectedSig = signBase64Url(payload);
        if (!MessageDigest.isEqual(expectedSig.getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
            throw new SecurityException("Invalid signed URL signature");
        }
        String[] fields = payload.split("\\|", -1);
        if (fields.length != 4) {
            throw new SecurityException("Malformed signed URL payload");
        }
        long expiresEpoch = Long.parseLong(fields[3]);
        if (clock.instant().isAfter(Instant.ofEpochSecond(expiresEpoch))) {
            throw new SecurityException("Signed download URL has expired");
        }
        return new VerifiedToken(fields[0], fields[1], fields[2], Instant.ofEpochSecond(expiresEpoch));
    }

    private String signBase64Url(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecretBytes, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign download token", e);
        }
    }

    public record VerifiedToken(String bucket, String objectKey, String authorizedActorId, Instant expiresAt) {
    }
}

