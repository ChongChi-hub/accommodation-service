package vn.cozynest.booking.domain.model;

import java.time.Instant;

public class AppointmentStatusHistory {

    private Long id;
    private String appointmentId;
    private String fromStatus;
    private String toStatus;
    private String actorId;
    private String actorRole;
    private String reason;
    private Instant createdAt = Instant.now();

    public AppointmentStatusHistory() {}

    public AppointmentStatusHistory(String appointmentId, String fromStatus, String toStatus,
                                    String actorId, String actorRole, String reason) {
        this.appointmentId = appointmentId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
