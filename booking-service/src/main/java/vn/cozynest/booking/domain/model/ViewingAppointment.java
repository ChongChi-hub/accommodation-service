package vn.cozynest.booking.domain.model;

import vn.cozynest.booking.domain.exception.InvalidAppointmentStateException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

public class ViewingAppointment {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private String id;
    private String roomId;
    private String tenantId;
    private String landlordId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private Instant scheduledStart;
    private Instant scheduledEnd;
    private String message;
    private String tenantNote;
    private String landlordNote;
    private AppointmentStatus status = AppointmentStatus.PENDING;
    private CancelReasonCode cancelReasonCode;
    private String cancelReasonNote;
    private String rejectionReason;
    private CancelledBy cancelledBy;
    private Instant confirmedAt;
    private Instant completedAt;
    private Long version = 0L;
    private String roomTitle;
    private String roomAddress;
    private String roomCoverThumbKey;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public ViewingAppointment() {}

    public void confirm(String actorId, String note) {
        if (this.status != AppointmentStatus.PENDING) {
            throw new InvalidAppointmentStateException(this.status, AppointmentStatus.CONFIRMED);
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
            throw new InvalidAppointmentStateException(this.status, AppointmentStatus.DECLINED);
        }
        validateReason(reasonCode, reasonNote);
        this.status = AppointmentStatus.DECLINED;
        this.cancelReasonCode = reasonCode;
        this.cancelReasonNote = reasonNote != null ? reasonNote.trim() : null;
        this.rejectionReason = this.cancelReasonNote;
        this.cancelledBy = CancelledBy.LANDLORD;
        this.updatedAt = Instant.now();
    }

    public void cancel(String actorId, CancelledBy by, CancelReasonCode reasonCode, String reasonNote) {
        if (this.status != AppointmentStatus.PENDING && this.status != AppointmentStatus.CONFIRMED) {
            throw new InvalidAppointmentStateException(this.status, AppointmentStatus.CANCELLED);
        }
        validateReason(reasonCode, reasonNote);
        this.status = AppointmentStatus.CANCELLED;
        this.cancelReasonCode = reasonCode;
        this.cancelReasonNote = reasonNote != null ? reasonNote.trim() : null;
        this.rejectionReason = this.cancelReasonNote;
        this.cancelledBy = by;
        this.updatedAt = Instant.now();
    }

    public void complete(String actorId, AppointmentStatus outcome, String note) {
        if (this.status != AppointmentStatus.CONFIRMED) {
            throw new InvalidAppointmentStateException(this.status, outcome);
        }
        if (outcome != AppointmentStatus.COMPLETED && outcome != AppointmentStatus.NO_SHOW) {
            throw new InvalidAppointmentStateException(this.status, outcome);
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
            throw new InvalidAppointmentStateException(this.status, AppointmentStatus.EXPIRED);
        }
        this.status = AppointmentStatus.EXPIRED;
        this.cancelledBy = CancelledBy.SYSTEM;
        this.updatedAt = Instant.now();
    }

    private void validateReason(CancelReasonCode code, String note) {
        if (code == null) {
            throw new IllegalArgumentException("Ma ly do khong duoc de trong");
        }
        if (code == CancelReasonCode.OTHER) {
            if (note == null || note.trim().length() < 10) {
                throw new IllegalArgumentException("Ly do khac bat buoc nhap toi thieu 10 ky tu");
            }
        }
        if (note != null && note.trim().length() > 500) {
            throw new IllegalArgumentException("Ghi chu ly do khong duoc vuot qua 500 ky tu");
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
