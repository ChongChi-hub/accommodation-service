package vn.cozynest.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class OAuthAccount {
    private final String id;
    private final String userId;
    private final String provider;
    private final String providerUserId;
    private final String email;
    private final Instant createdAt;

    public OAuthAccount(String id, String userId, String provider, String providerUserId, String email, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static OAuthAccount create(String userId, String provider, String providerUserId, String email) {
        return new OAuthAccount(UUID.randomUUID().toString(), userId, provider, providerUserId, email, Instant.now());
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getProvider() { return provider; }
    public String getProviderUserId() { return providerUserId; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }
}

