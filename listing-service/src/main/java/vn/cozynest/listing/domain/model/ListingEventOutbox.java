package vn.cozynest.listing.domain.model;

import java.time.Instant;
import java.util.UUID;

public class ListingEventOutbox {

    private final String id;
    private final String aggregateType;
    private final String aggregateId;
    private final String eventType;
    private final String payload;
    private String status;
    private int retryCount;
    private final Instant createdAt;
    private Instant processedAt;

    public ListingEventOutbox(
            String id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            String status,
            int retryCount,
            Instant createdAt,
            Instant processedAt
    ) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.aggregateType = aggregateType != null ? aggregateType : "ROOM";
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status != null ? status : "PENDING";
        this.retryCount = retryCount;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.processedAt = processedAt;
    }

    public static ListingEventOutbox create(String aggregateType, String aggregateId, String eventType, String payload) {
        return new ListingEventOutbox(
                UUID.randomUUID().toString(),
                aggregateType,
                aggregateId,
                eventType,
                payload,
                "PENDING",
                0,
                Instant.now(),
                null
        );
    }

    public void markProcessed() {
        this.status = "PROCESSED";
        this.processedAt = Instant.now();
    }

    public void markFailedAttempt() {
        this.retryCount++;
    }

    public String getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public String getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
}
