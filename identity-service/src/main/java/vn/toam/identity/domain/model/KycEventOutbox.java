package vn.toam.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class KycEventOutbox {

    private final String id;
    private final String aggregateType;
    private final String aggregateId;
    private final String eventType;
    private final String payload;
    private String status;
    private final Instant createdAt;
    private Instant processedAt;

    public KycEventOutbox(String id, String aggregateType, String aggregateId, String eventType,
                          String payload, String status, Instant createdAt, Instant processedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.aggregateType = aggregateType != null ? aggregateType : "KYC_VERIFICATION";
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status != null ? status : "PENDING";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.processedAt = processedAt;
    }

    public static KycEventOutbox create(String aggregateId, String eventType, String payload) {
        return new KycEventOutbox(
                UUID.randomUUID().toString(),
                "KYC_VERIFICATION",
                aggregateId,
                eventType,
                payload,
                "PENDING",
                Instant.now(),
                null
        );
    }

    public void markProcessed() {
        this.status = "PROCESSED";
        this.processedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
}

