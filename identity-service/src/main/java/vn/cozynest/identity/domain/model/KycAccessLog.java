package vn.cozynest.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class KycAccessLog {

    private final String id;
    private final String verificationId;
    private final String actorId;
    private final String actorRole;
    private final KycAccessAction action;
    private final String ip;
    private final Instant createdAt;

    public KycAccessLog(String id, String verificationId, String actorId, String actorRole,
                        KycAccessAction action, String ip, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.verificationId = verificationId;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.action = action;
        this.ip = ip;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static KycAccessLog record(String verificationId, String actorId, String actorRole,
                                      KycAccessAction action, String ip) {
        return new KycAccessLog(
                UUID.randomUUID().toString(),
                verificationId,
                actorId,
                actorRole,
                action,
                ip,
                Instant.now()
        );
    }

    public String getId() { return id; }
    public String getVerificationId() { return verificationId; }
    public String getActorId() { return actorId; }
    public String getActorRole() { return actorRole; }
    public KycAccessAction getAction() { return action; }
    public String getIp() { return ip; }
    public Instant getCreatedAt() { return createdAt; }
}

