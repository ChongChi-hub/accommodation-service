package vn.cozynest.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class IdentityAuditLog {
    private final String id;
    private final String userId;
    private final String actorId;
    private final String eventType;
    private final String provider;
    private final String ipAddress;
    private final String userAgent;
    private final String details;
    private final Instant createdAt;

    public IdentityAuditLog(String id, String userId, String actorId, String eventType,
                            String provider, String ipAddress, String userAgent,
                            String details, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.userId = userId;
        this.actorId = actorId;
        this.eventType = eventType;
        this.provider = provider;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.details = details;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static IdentityAuditLog create(String userId, String actorId, String eventType,
                                          String provider, String ipAddress, String userAgent,
                                          String details) {
        return new IdentityAuditLog(
                UUID.randomUUID().toString(),
                userId,
                actorId,
                eventType,
                provider,
                ipAddress,
                userAgent,
                details,
                Instant.now()
        );
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getActorId() { return actorId; }
    public String getEventType() { return eventType; }
    public String getProvider() { return provider; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
