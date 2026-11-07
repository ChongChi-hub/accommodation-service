package vn.toam.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class RefreshToken {
    private final String id;
    private final String userId;
    private final String tokenHash;
    private final String deviceInfo;
    private final String ipAddress;
    private final Instant expiresAt;
    private Instant revokedAt;
    private String replacedBy;
    private final Instant createdAt;

    public RefreshToken(String id, String userId, String tokenHash, String deviceInfo,
                        String ipAddress, Instant expiresAt, Instant revokedAt,
                        String replacedBy, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.replacedBy = replacedBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static RefreshToken create(String userId, String tokenHash, String deviceInfo, String ipAddress, Instant expiresAt) {
        return new RefreshToken(
                UUID.randomUUID().toString(),
                userId,
                tokenHash,
                deviceInfo,
                ipAddress,
                expiresAt,
                null,
                null,
                Instant.now()
        );
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }

    public void revoke(String replacementTokenHash) {
        this.revokedAt = Instant.now();
        this.replacedBy = replacementTokenHash;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public String getDeviceInfo() { return deviceInfo; }
    public String getIpAddress() { return ipAddress; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public String getReplacedBy() { return replacedBy; }
    public Instant getCreatedAt() { return createdAt; }
}

