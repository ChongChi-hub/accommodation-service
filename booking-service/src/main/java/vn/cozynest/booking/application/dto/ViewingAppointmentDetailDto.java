package vn.cozynest.booking.application.dto;

import vn.cozynest.booking.domain.model.AppointmentStatus;
import vn.cozynest.booking.domain.model.CancelReasonCode;
import vn.cozynest.booking.domain.model.CancelledBy;
import vn.cozynest.booking.domain.model.ViewingAppointment;

import java.time.Instant;
import java.util.List;

public record ViewingAppointmentDetailDto(
        String id,
        String roomId,
        String tenantId,
        String landlordId,
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
        Instant createdAt,
        List<AppointmentStatusHistoryDto> history
) {
    public static ViewingAppointmentDetailDto fromEntity(
            ViewingAppointment entity,
            List<AppointmentStatusHistoryDto> history
    ) {
        boolean canSeePhone = entity.getStatus() == AppointmentStatus.CONFIRMED
                || entity.getStatus() == AppointmentStatus.COMPLETED;
        String phone = canSeePhone
                ? entity.getContactPhone()
                : PhoneMasker.mask(entity.getContactPhone());

        return new ViewingAppointmentDetailDto(
                entity.getId(),
                entity.getRoomId(),
                entity.getTenantId(),
                entity.getLandlordId(),
                entity.getRoomTitle(),
                entity.getRoomAddress(),
                entity.getRoomCoverThumbKey(),
                entity.getScheduledStart(),
                entity.getScheduledEnd(),
                entity.getStatus(),
                entity.getContactName(),
                phone,
                entity.getContactEmail(),
                entity.getTenantNote(),
                entity.getLandlordNote(),
                entity.getCancelReasonCode(),
                entity.getCancelReasonNote(),
                entity.getCancelledBy(),
                entity.getConfirmedAt(),
                entity.getCompletedAt(),
                entity.getVersion(),
                entity.getCreatedAt(),
                history != null ? history : List.of()
        );
    }
}
