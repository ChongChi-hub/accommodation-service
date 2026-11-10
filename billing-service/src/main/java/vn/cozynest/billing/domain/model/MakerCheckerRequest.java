package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.util.UUID;

public class MakerCheckerRequest {
    private final String id;
    private final String makerId;
    private String checkerId;
    private final String actionType;
    private final long amount;
    private String status;
    private final String reason;
    private final String payloadJson;
    private Instant decidedAt;
    private final Instant createdAt;

    public MakerCheckerRequest(String id, String makerId, String checkerId, String actionType,
                               long amount, String status, String reason, String payloadJson,
                               Instant decidedAt, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.makerId = makerId;
        this.checkerId = checkerId;
        this.actionType = actionType;
        this.amount = amount;
        this.status = status != null ? status : "PENDING";
        this.reason = reason;
        this.payloadJson = payloadJson;
        this.decidedAt = decidedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static MakerCheckerRequest create(String makerId, String actionType, long amount,
                                             String reason, String payloadJson) {
        return new MakerCheckerRequest(
                UUID.randomUUID().toString(),
                makerId,
                null,
                actionType,
                amount,
                "PENDING",
                reason,
                payloadJson,
                null,
                Instant.now()
        );
    }

    public void approve(String reviewerId) {
        if (this.makerId.equals(reviewerId)) {
            throw new IllegalStateException("Quy tac Maker-Checker: Nguoi duyet khong the la nguoi tao yeu cau");
        }
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("Yeu cau da duoc xu ly truoc do");
        }
        this.checkerId = reviewerId;
        this.status = "APPROVED";
        this.decidedAt = Instant.now();
    }

    public void reject(String reviewerId, String reason) {
        if (this.makerId.equals(reviewerId)) {
            throw new IllegalStateException("Quy tac Maker-Checker: Nguoi duyet khong the la nguoi tao yeu cau");
        }
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("Yeu cau da duoc xu ly truoc do");
        }
        this.checkerId = reviewerId;
        this.status = "REJECTED";
        this.decidedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getMakerId() { return makerId; }
    public String getCheckerId() { return checkerId; }
    public String getActionType() { return actionType; }
    public long getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public String getPayloadJson() { return payloadJson; }
    public Instant getDecidedAt() { return decidedAt; }
    public Instant getCreatedAt() { return createdAt; }
}

