package vn.cozynest.booking.domain.model;

import java.time.Instant;
import java.util.UUID;

public class AppointmentEventOutbox {

    private String id;
    private String eventType;
    private String aggregateId;
    private String payload;
    private boolean processed = false;
    private Instant createdAt = Instant.now();
    private Instant processedAt;

    public AppointmentEventOutbox() {
        this.id = UUID.randomUUID().toString();
    }

    public AppointmentEventOutbox(String eventType, String aggregateId, String payload) {
        this.id = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.processed = false;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
