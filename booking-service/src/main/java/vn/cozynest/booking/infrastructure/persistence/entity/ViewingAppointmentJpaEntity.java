package vn.cozynest.booking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.cozynest.booking.domain.model.AppointmentStatus;
import vn.cozynest.booking.domain.model.CancelReasonCode;
import vn.cozynest.booking.domain.model.CancelledBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "viewing_appointments")
public class ViewingAppointmentJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "room_id", length = 36, nullable = false)
    private String roomId;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "landlord_id", length = 36, nullable = false)
    private String landlordId;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "appointment_time", nullable = false)
    private LocalTime appointmentTime;

    @Column(name = "scheduled_start", nullable = false)
    private Instant scheduledStart;

    @Column(name = "scheduled_end", nullable = false)
    private Instant scheduledEnd;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "tenant_note", columnDefinition = "TEXT")
    private String tenantNote;

    @Column(name = "landlord_note", columnDefinition = "TEXT")
    private String landlordNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason_code", length = 50)
    private CancelReasonCode cancelReasonCode;

    @Column(name = "cancel_reason_note", length = 500)
    private String cancelReasonNote;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by", length = 30)
    private CancelledBy cancelledBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "room_title", length = 255)
    private String roomTitle;

    @Column(name = "room_address", length = 300)
    private String roomAddress;

    @Column(name = "room_cover_thumb_key", length = 500)
    private String roomCoverThumbKey;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}

