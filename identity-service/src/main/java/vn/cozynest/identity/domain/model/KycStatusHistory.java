package vn.cozynest.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class KycStatusHistory {

    private final String id;
    private final String verificationId;
    private final String fromStatus;
    private final String toStatus;
    private final String actorId;
    private final String reason;
    private final Instant createdAt;

    public KycStatusHistory(String id, String verificationId, String fromStatus, String toStatus,
                            String actorId, String reason, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.verificationId = verificationId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorId = actorId;
        this.reason = reason;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static KycStatusHistory record(String verificationId, String fromStatus, String toStatus,
                                          String actorId, String reason) {
        return new KycStatusHistory(
                UUID.randomUUID().toString(),
                verificationId,
                fromStatus,
                toStatus,
                actorId,
                reason,
                Instant.now()
        );
    }

    public String getId() { return id; }
    public String getVerificationId() { return verificationId; }
    public String getFromStatus() { return fromStatus; }
    public String getToStatus() { return toStatus; }
    public String getActorId() { return actorId; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}

