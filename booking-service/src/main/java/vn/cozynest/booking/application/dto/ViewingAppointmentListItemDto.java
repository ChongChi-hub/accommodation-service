package vn.cozynest.booking.application.dto;

import vn.cozynest.booking.domain.model.AppointmentStatus;
import vn.cozynest.booking.domain.model.CancelReasonCode;
import vn.cozynest.booking.domain.model.CancelledBy;
import vn.cozynest.booking.domain.model.ViewingAppointment;

import java.time.Instant;

public record ViewingAppointmentListItemDto(
        String id,
        String roomId,
        String roomTitle,
        String roomAddress,
        String roomCoverThumbKey,
        Instant scheduledStart,
        Instant scheduledEnd,
        AppointmentStatus status,
        String contactName,
        String contactPhone,
        String contactEmail,
        String tenantNote,
        String landlordNote,
        CancelReasonCode cancelReasonCode,
        String cancelReasonNote,
        CancelledBy cancelledBy,
        Instant confirmedAt,
        Instant completedAt,
        Long version,
        Instant createdAt
) {
    public static ViewingAppointmentListItemDto fromEntity(ViewingAppointment entity) {
        boolean canSeeContact = entity.getStatus() == AppointmentStatus.CONFIRMED
                || entity.getStatus() == AppointmentStatus.COMPLETED;
        String phone = canSeeContact
                ? entity.getContactPhone()
                : PhoneMasker.mask(entity.getContactPhone());
        String email = canSeeContact
                ? entity.getContactEmail()
                : PhoneMasker.maskEmail(entity.getContactEmail());

        return new ViewingAppointmentListItemDto(
                entity.getId(),
                entity.getRoomId(),
                entity.getRoomTitle(),
                entity.getRoomAddress(),
                entity.getRoomCoverThumbKey(),
                entity.getScheduledStart(),
                entity.getScheduledEnd(),
                entity.getStatus(),
                entity.getContactName(),
                phone,
                email,
                entity.getTenantNote(),
                entity.getLandlordNote(),
                entity.getCancelReasonCode(),
                entity.getCancelReasonNote(),
                entity.getCancelledBy(),
                entity.getConfirmedAt(),
                entity.getCompletedAt(),
                entity.getVersion(),
                entity.getCreatedAt()
        );
    }
}
