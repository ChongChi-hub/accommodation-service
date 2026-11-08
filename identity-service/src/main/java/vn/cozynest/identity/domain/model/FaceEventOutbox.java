package vn.cozynest.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class FaceEventOutbox {

    private final String id;
    private final String aggregateId;
    private final String eventType;
    private final String payload;
    private boolean published;
    private final Instant createdAt;

    public FaceEventOutbox(String id, String aggregateId, String eventType, String payload, boolean published, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.published = published;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static FaceEventOutbox create(String aggregateId, String eventType, String payload) {
        return new FaceEventOutbox(UUID.randomUUID().toString(), aggregateId, eventType, payload, false, Instant.now());
    }

    public void markPublished() {
        this.published = true;
    }

    public String getId() { return id; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public boolean isPublished() { return published; }
    public Instant getCreatedAt() { return createdAt; }
}

