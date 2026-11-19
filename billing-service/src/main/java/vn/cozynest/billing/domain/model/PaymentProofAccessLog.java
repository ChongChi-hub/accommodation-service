package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.util.UUID;

public class PaymentProofAccessLog {
    private final String id;
    private final String proofId;
    private final String paymentCode;
    private final String actorId;
    private final String actorRole;
    private final String action;
    private final String ipAddress;
    private final Instant createdAt;

    public PaymentProofAccessLog(String id, String proofId, String paymentCode,
                                 String actorId, String actorRole, String action,
                                 String ipAddress, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.proofId = proofId;
        this.paymentCode = paymentCode;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.action = action;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static PaymentProofAccessLog create(String proofId, String paymentCode,
                                               String actorId, String actorRole,
                                               String action, String ipAddress) {
        return new PaymentProofAccessLog(UUID.randomUUID().toString(), proofId, paymentCode,
                actorId, actorRole, action, ipAddress, Instant.now());
    }

    public String getId() { return id; }
    public String getProofId() { return proofId; }
    public String getPaymentCode() { return paymentCode; }
    public String getActorId() { return actorId; }
    public String getActorRole() { return actorRole; }
    public String getAction() { return action; }
    public String getIpAddress() { return ipAddress; }
    public Instant getCreatedAt() { return createdAt; }
}

