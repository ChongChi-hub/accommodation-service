package vn.cozynest.booking.infrastructure.persistence.mapper;

import vn.cozynest.booking.domain.model.AppointmentEventOutbox;
import vn.cozynest.booking.domain.model.AppointmentStatusHistory;
import vn.cozynest.booking.domain.model.ViewingAppointment;
import vn.cozynest.booking.infrastructure.persistence.entity.AppointmentEventOutboxJpaEntity;
import vn.cozynest.booking.infrastructure.persistence.entity.AppointmentStatusHistoryJpaEntity;
import vn.cozynest.booking.infrastructure.persistence.entity.ViewingAppointmentJpaEntity;

public final class AppointmentPersistenceMapper {

    private AppointmentPersistenceMapper() {}

    public static ViewingAppointment toDomain(ViewingAppointmentJpaEntity entity) {
        if (entity == null) return null;
        ViewingAppointment domain = new ViewingAppointment();
        domain.setId(entity.getId());
        domain.setRoomId(entity.getRoomId());
        domain.setTenantId(entity.getTenantId());
        domain.setLandlordId(entity.getLandlordId());
        domain.setAppointmentDate(entity.getAppointmentDate());
        domain.setAppointmentTime(entity.getAppointmentTime());
        domain.setScheduledStart(entity.getScheduledStart());
        domain.setScheduledEnd(entity.getScheduledEnd());
        domain.setMessage(entity.getMessage());
        domain.setTenantNote(entity.getTenantNote());
        domain.setLandlordNote(entity.getLandlordNote());
        domain.setStatus(entity.getStatus());
        domain.setCancelReasonCode(entity.getCancelReasonCode());
        domain.setCancelReasonNote(entity.getCancelReasonNote());
        domain.setRejectionReason(entity.getRejectionReason());
        domain.setCancelledBy(entity.getCancelledBy());
        domain.setConfirmedAt(entity.getConfirmedAt());
        domain.setCompletedAt(entity.getCompletedAt());
        domain.setVersion(entity.getVersion());
        domain.setRoomTitle(entity.getRoomTitle());
        domain.setRoomAddress(entity.getRoomAddress());
        domain.setRoomCoverThumbKey(entity.getRoomCoverThumbKey());
        domain.setContactName(entity.getContactName());
        domain.setContactPhone(entity.getContactPhone());
        domain.setContactEmail(entity.getContactEmail());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }

    public static ViewingAppointmentJpaEntity toEntity(ViewingAppointment domain) {
        if (domain == null) return null;
        ViewingAppointmentJpaEntity entity = new ViewingAppointmentJpaEntity();
        entity.setId(domain.getId());
        entity.setRoomId(domain.getRoomId());
        entity.setTenantId(domain.getTenantId());
        entity.setLandlordId(domain.getLandlordId());
        entity.setAppointmentDate(domain.getAppointmentDate());
        entity.setAppointmentTime(domain.getAppointmentTime());
        entity.setScheduledStart(domain.getScheduledStart());
        entity.setScheduledEnd(domain.getScheduledEnd());
        entity.setMessage(domain.getMessage());
        entity.setTenantNote(domain.getTenantNote());
        entity.setLandlordNote(domain.getLandlordNote());
        entity.setStatus(domain.getStatus());
        entity.setCancelReasonCode(domain.getCancelReasonCode());
        entity.setCancelReasonNote(domain.getCancelReasonNote());
        entity.setRejectionReason(domain.getRejectionReason());
        entity.setCancelledBy(domain.getCancelledBy());
        entity.setConfirmedAt(domain.getConfirmedAt());
        entity.setCompletedAt(domain.getCompletedAt());
        entity.setVersion(domain.getVersion());
        entity.setRoomTitle(domain.getRoomTitle());
        entity.setRoomAddress(domain.getRoomAddress());
        entity.setRoomCoverThumbKey(domain.getRoomCoverThumbKey());
        entity.setContactName(domain.getContactName());
        entity.setContactPhone(domain.getContactPhone());
        entity.setContactEmail(domain.getContactEmail());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static AppointmentStatusHistory toDomain(AppointmentStatusHistoryJpaEntity entity) {
        if (entity == null) return null;
        AppointmentStatusHistory domain = new AppointmentStatusHistory();
        domain.setId(entity.getId());
        domain.setAppointmentId(entity.getAppointmentId());
        domain.setFromStatus(entity.getFromStatus());
        domain.setToStatus(entity.getToStatus());
        domain.setActorId(entity.getActorId());
        domain.setActorRole(entity.getActorRole());
        domain.setReason(entity.getReason());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public static AppointmentStatusHistoryJpaEntity toEntity(AppointmentStatusHistory domain) {
        if (domain == null) return null;
        AppointmentStatusHistoryJpaEntity entity = new AppointmentStatusHistoryJpaEntity();
        entity.setId(domain.getId());
        entity.setAppointmentId(domain.getAppointmentId());
        entity.setFromStatus(domain.getFromStatus());
        entity.setToStatus(domain.getToStatus());
        entity.setActorId(domain.getActorId());
        entity.setActorRole(domain.getActorRole());
        entity.setReason(domain.getReason());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public static AppointmentEventOutbox toDomain(AppointmentEventOutboxJpaEntity entity) {
        if (entity == null) return null;
        AppointmentEventOutbox domain = new AppointmentEventOutbox();
        domain.setId(entity.getEventId());
        domain.setEventType(entity.getEventType());
        domain.setAggregateId(entity.getAggregateId());
        domain.setPayload(entity.getPayload());
        domain.setProcessed(entity.getPublishedAt() != null);
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setProcessedAt(entity.getPublishedAt());
        return domain;
    }

    public static AppointmentEventOutboxJpaEntity toEntity(AppointmentEventOutbox domain) {
        if (domain == null) return null;
        AppointmentEventOutboxJpaEntity entity = new AppointmentEventOutboxJpaEntity();
        if (domain.getId() != null) {
            entity.setEventId(domain.getId());
        }
        entity.setEventType(domain.getEventType());
        entity.setAggregateId(domain.getAggregateId());
        entity.setPayload(domain.getPayload());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setPublishedAt(domain.getProcessedAt());
        return entity;
    }
}
