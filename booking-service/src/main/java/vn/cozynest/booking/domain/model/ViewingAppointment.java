package vn.cozynest.booking.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Entity
@Table(name = "viewing_appointments")
public class ViewingAppointment {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

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

    public ViewingAppointment() {}

    public void confirm(String actorId, String note) {
        if (this.status != AppointmentStatus.PENDING) {
            throw new IllegalStateException("Chi co the xac nhan lich hen o trang thai PENDING");
        }
        this.status = AppointmentStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
        if (note != null && !note.isBlank()) {
            this.landlordNote = note.trim();
        }
        this.updatedAt = Instant.now();
    }

    public void decline(String actorId, CancelReasonCode reasonCode, String reasonNote) {
        if (this.status != AppointmentStatus.PENDING) {
            throw new IllegalStateException("Chi co the tu choi lich hen o trang thai PENDING");
        }
        validateReasonNote(reasonCode, reasonNote);
        this.status = AppointmentStatus.DECLINED;
        this.cancelReasonCode = reasonCode;
        this.cancelReasonNote = reasonNote != null ? reasonNote.trim() : null;
        this.rejectionReason = this.cancelReasonNote;
        this.cancelledBy = CancelledBy.LANDLORD;
        this.updatedAt = Instant.now();
    }

    public void cancel(String actorId, CancelledBy by, CancelReasonCode reasonCode, String reasonNote) {
        if (this.status != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateException("Chi co the huy lich hen o trang thai CONFIRMED");
        }
        validateReasonNote(reasonCode, reasonNote);
        this.status = AppointmentStatus.CANCELLED;
        this.cancelReasonCode = reasonCode;
        this.cancelReasonNote = reasonNote != null ? reasonNote.trim() : null;
        this.rejectionReason = this.cancelReasonNote;
        this.cancelledBy = by;
        this.updatedAt = Instant.now();
    }

    public void complete(String actorId, AppointmentStatus outcome, String note) {
        if (this.status != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateException("Chi co the cap nhat ket qua khi lich hen o trang thai CONFIRMED");
        }
        if (outcome != AppointmentStatus.COMPLETED && outcome != AppointmentStatus.NO_SHOW) {
            throw new IllegalArgumentException("Ket qua lich hen phai la COMPLETED hoac NO_SHOW");
        }
        if (Instant.now().isBefore(this.scheduledStart)) {
            throw new IllegalStateException("Khong the cap nhat ket qua truoc gio hen bat dau");
        }
        this.status = outcome;
        this.completedAt = Instant.now();
        if (note != null && !note.isBlank()) {
            this.landlordNote = note.trim();
        }
        this.updatedAt = Instant.now();
    }

    public void expire() {
        if (this.status != AppointmentStatus.PENDING) {
            throw new IllegalStateException("Chi co the het han lich hen o trang thai PENDING");
        }
        if (Instant.now().isBefore(this.scheduledStart)) {
            throw new IllegalStateException("Khong the het han truoc gio hen bat dau");
        }
        this.status = AppointmentStatus.EXPIRED;
        this.cancelledBy = CancelledBy.SYSTEM;
        this.updatedAt = Instant.now();
    }

    private void validateReasonNote(CancelReasonCode code, String note) {
        if (code == CancelReasonCode.OTHER) {
            if (note == null || note.trim().length() < 10) {
                throw new IllegalArgumentException("Ly do khac bat buoc nhap toi thieu 10 ky tu");
            }
        }
    }

    public void syncLegacyDateTimes() {
        if (this.scheduledStart != null) {
            var zdt = this.scheduledStart.atZone(VN_ZONE);
            this.appointmentDate = zdt.toLocalDate();
            this.appointmentTime = zdt.toLocalTime();
        }
        if (this.tenantNote != null && this.message == null) {
            this.message = this.tenantNote;
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public LocalTime getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(LocalTime appointmentTime) { this.appointmentTime = appointmentTime; }
    public Instant getScheduledStart() { return scheduledStart; }
    public void setScheduledStart(Instant scheduledStart) { this.scheduledStart = scheduledStart; }
    public Instant getScheduledEnd() { return scheduledEnd; }
    public void setScheduledEnd(Instant scheduledEnd) { this.scheduledEnd = scheduledEnd; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTenantNote() { return tenantNote; }
    public void setTenantNote(String tenantNote) { this.tenantNote = tenantNote; }
    public String getLandlordNote() { return landlordNote; }
    public void setLandlordNote(String landlordNote) { this.landlordNote = landlordNote; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public CancelReasonCode getCancelReasonCode() { return cancelReasonCode; }
    public void setCancelReasonCode(CancelReasonCode cancelReasonCode) { this.cancelReasonCode = cancelReasonCode; }
    public String getCancelReasonNote() { return cancelReasonNote; }
    public void setCancelReasonNote(String cancelReasonNote) { this.cancelReasonNote = cancelReasonNote; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public CancelledBy getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(CancelledBy cancelledBy) { this.cancelledBy = cancelledBy; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getRoomTitle() { return roomTitle; }
    public void setRoomTitle(String roomTitle) { this.roomTitle = roomTitle; }
    public String getRoomAddress() { return roomAddress; }
    public void setRoomAddress(String roomAddress) { this.roomAddress = roomAddress; }
    public String getRoomCoverThumbKey() { return roomCoverThumbKey; }
    public void setRoomCoverThumbKey(String roomCoverThumbKey) { this.roomCoverThumbKey = roomCoverThumbKey; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
