package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.util.UUID;

public class BillingEventOutbox {
    private final String id;
    private final String eventType;
    private final String aggregateType;
    private final String aggregateId;
    private final String payloadJson;
    private String status;
    private int retryCount;
    private final Instant createdAt;
    private Instant publishedAt;

    public BillingEventOutbox(String id, String eventType, String aggregateType, String aggregateId,
                              String payloadJson, String status, int retryCount,
                              Instant createdAt, Instant publishedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payloadJson = payloadJson;
        this.status = status != null ? status : "PENDING";
        this.retryCount = retryCount;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.publishedAt = publishedAt;
    }

    public static BillingEventOutbox create(String eventType, String aggregateType, String aggregateId, String payloadJson) {
        return new BillingEventOutbox(UUID.randomUUID().toString(), eventType, aggregateType, aggregateId,
                payloadJson, "PENDING", 0, Instant.now(), null);
    }

    public void markPublished() {
        this.status = "PUBLISHED";
        this.publishedAt = Instant.now();
    }

    public void markFailedAttempt() {
        this.retryCount++;
        if (this.retryCount >= 5) {
            this.status = "DEAD_LETTER";
        }
    }

    public String getId() { return id; }
    public String getEventType() { return eventType; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getPayloadJson() { return payloadJson; }
    public String getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
}

