package vn.cozynest.identity.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

public class PhoneVerification {
    private final String id;
    private final String phoneNumber;
    private final String otpHash;
    private int attemptCount;
    private boolean verified;
    private final Instant expiresAt;
    private final Instant createdAt;

    public PhoneVerification(String id, String phoneNumber, String otpHash,
                             int attemptCount, boolean verified,
                             Instant expiresAt, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.phoneNumber = phoneNumber;
        this.otpHash = otpHash;
        this.attemptCount = attemptCount;
        this.verified = verified;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static PhoneVerification create(String phoneNumber, String plainOtp, long ttlSeconds) {
        String hash = hashOtp(plainOtp);
        return new PhoneVerification(
                UUID.randomUUID().toString(),
                phoneNumber,
                hash,
                0,
                false,
                Instant.now().plusSeconds(ttlSeconds),
                Instant.now()
        );
    }

    public boolean verify(String rawOtp) {
        if (verified || isExpired() || attemptCount >= 5) {
            return false;
        }
        attemptCount++;
        String candidateHash = hashOtp(rawOtp);
        if (this.otpHash.equalsIgnoreCase(candidateHash)) {
            this.verified = true;
            return true;
        }
        return false;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public static String hashOtp(String plainOtp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainOtp.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public String getId() { return id; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getOtpHash() { return otpHash; }
    public int getAttemptCount() { return attemptCount; }
    public boolean isVerified() { return verified; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}

