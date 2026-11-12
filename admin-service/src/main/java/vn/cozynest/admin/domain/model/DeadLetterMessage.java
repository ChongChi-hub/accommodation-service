package vn.cozynest.admin.domain.model;

import java.time.Instant;
import java.util.UUID;

public class DeadLetterMessage {
    private final String id;
    private final String eventId;
    private final String eventType;
    private final int schemaVersion;
    private final String aggregateType;
    private final String aggregateId;
    private final String sourceService;
    private final String payloadJson;
    private final String failureReason;
    private int retryCount;
    private String status;
    private String replayedBy;
    private Instant replayedAt;
    private final Instant createdAt;

    public DeadLetterMessage(String id, String eventId, String eventType, int schemaVersion,
                             String aggregateType, String aggregateId, String sourceService,
                             String payloadJson, String failureReason, int retryCount,
                             String status, String replayedBy, Instant replayedAt, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.eventId = eventId;
        this.eventType = eventType;
        this.schemaVersion = schemaVersion > 0 ? schemaVersion : 1;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.sourceService = sourceService;
        this.payloadJson = payloadJson;
        this.failureReason = failureReason;
        this.retryCount = retryCount;
        this.status = status != null ? status : "PENDING_REPLAY";
        this.replayedBy = replayedBy;
        this.replayedAt = replayedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public void markReplayed(String actorId) {
        if ("REPLAYED".equals(this.status)) {
            throw new IllegalStateException("Su kien DLQ da duoc phat lai truoc do");
        }
        this.retryCount += 1;
        this.status = "REPLAYED";
        this.replayedBy = actorId;
        this.replayedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public int getSchemaVersion() { return schemaVersion; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getSourceService() { return sourceService; }
    public String getPayloadJson() { return payloadJson; }
    public String getFailureReason() { return failureReason; }
    public int getRetryCount() { return retryCount; }
    public String getStatus() { return status; }
    public String getReplayedBy() { return replayedBy; }
    public Instant getReplayedAt() { return replayedAt; }
    public Instant getCreatedAt() { return createdAt; }
}

