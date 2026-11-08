package vn.cozynest.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class FaceAccessLog {

    private final String id;
    private final String actorId;
    private final String actorRole;
    private final String frameId;
    private final String faceVerificationId;
    private final String purpose;
    private final String ipAddress;
    private final String userAgent;
    private final Instant createdAt;

    public FaceAccessLog(
            String id, String actorId, String actorRole, String frameId,
            String faceVerificationId, String purpose, String ipAddress,
            String userAgent, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.frameId = frameId;
        this.faceVerificationId = faceVerificationId;
        this.purpose = purpose;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static FaceAccessLog record(
            String actorId, String actorRole, String frameId,
            String faceVerificationId, String purpose, String ipAddress, String userAgent) {
        return new FaceAccessLog(
                UUID.randomUUID().toString(), actorId, actorRole, frameId,
                faceVerificationId, purpose, ipAddress, userAgent, Instant.now()
        );
    }

    public String getId() { return id; }
    public String getActorId() { return actorId; }
    public String getActorRole() { return actorRole; }
    public String getFrameId() { return frameId; }
    public String getFaceVerificationId() { return faceVerificationId; }
    public String getPurpose() { return purpose; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public Instant getCreatedAt() { return createdAt; }
}

