package vn.cozynest.booking.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.booking.application.dto.CancelAppointmentRequest;
import vn.cozynest.booking.application.dto.CreateAppointmentRequest;
import vn.cozynest.booking.application.dto.ViewingAppointmentListItemDto;
import vn.cozynest.booking.application.exception.AppointmentNotFoundException;
import vn.cozynest.booking.application.exception.OptimisticLockConflictException;
import vn.cozynest.booking.domain.model.*;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.booking.domain.repository.AppointmentStatusHistoryRepository;
import vn.cozynest.booking.domain.repository.ViewingAppointmentRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TenantAppointmentService {

    private final ViewingAppointmentRepository appointmentRepository;
    private final Optional<AppointmentStatusHistoryRepository> historyRepository;
    private final Optional<AppointmentEventOutboxRepository> outboxRepository;
    private final Optional<ObjectMapper> objectMapper;

    public TenantAppointmentService(ViewingAppointmentRepository appointmentRepository) {
        this(appointmentRepository, null, null, null);
    }

    @Autowired
    public TenantAppointmentService(
            ViewingAppointmentRepository appointmentRepository,
            AppointmentStatusHistoryRepository historyRepository,
            AppointmentEventOutboxRepository outboxRepository,
            ObjectMapper objectMapper
    ) {
        this.appointmentRepository = appointmentRepository;
        this.historyRepository = Optional.ofNullable(historyRepository);
        this.outboxRepository = Optional.ofNullable(outboxRepository);
        this.objectMapper = Optional.ofNullable(objectMapper);
    }

    public Page<ViewingAppointmentListItemDto> getTenantAppointments(String tenantId, int page, int size) {
        int pageNum = Math.max(0, page);
        int pageSize = size > 0 ? size : 20;
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "scheduledStart"));

        List<ViewingAppointment> items = appointmentRepository.findByTenantId(tenantId, pageNum, pageSize);
        long total = appointmentRepository.countByTenantId(tenantId);
        List<ViewingAppointmentListItemDto> dtos = items.stream().map(ViewingAppointmentListItemDto::fromEntity).toList();
        return new PageImpl<>(dtos, pageable, total);
    }

    @Transactional
    public ViewingAppointmentListItemDto createAppointment(String tenantId, CreateAppointmentRequest req) {
        Instant now = Instant.now();
        if (req.scheduledStart().isBefore(now.minusSeconds(60))) {
            throw new IllegalArgumentException("Thoi gian dat lich xem phong phai o tuong lai");
        }
        Instant end = req.scheduledEnd() != null ? req.scheduledEnd() : req.scheduledStart().plusSeconds(45L * 60L);
        if (!end.isAfter(req.scheduledStart())) {
            throw new IllegalArgumentException("Thoi gian ket thuc phai sau thoi gian bat dau");
        }
        if (tenantId.equals(req.landlordId())) {
            throw new IllegalArgumentException("Chu tro khong the tu dat lich xem phong cua chinh minh");
        }

        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setId(UUID.randomUUID().toString());
        appointment.setRoomId(req.roomId().trim());
        appointment.setTenantId(tenantId);
        appointment.setLandlordId(req.landlordId().trim());
        appointment.setScheduledStart(req.scheduledStart());
        appointment.setScheduledEnd(end);
        appointment.setTenantNote(req.tenantNote() != null ? req.tenantNote().trim() : null);
        appointment.setMessage(appointment.getTenantNote());
        appointment.setRoomTitle(req.roomTitle() != null && !req.roomTitle().isBlank() ? req.roomTitle().trim() : "Phong #" + req.roomId());
        appointment.setRoomAddress(req.roomAddress());
        appointment.setRoomCoverThumbKey(req.roomCoverThumbKey());
        appointment.setContactName(req.contactName());
        appointment.setContactPhone(req.contactPhone());
        appointment.setContactEmail(req.contactEmail());
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.syncLegacyDateTimes();

        try {
            ViewingAppointment saved = appointmentRepository.save(appointment);
            historyRepository.ifPresent(repo -> repo.save(new AppointmentStatusHistory(
                    saved.getId(), null, "PENDING", tenantId, "TENANT", saved.getTenantNote())));
            recordOutbox("APPOINTMENT_CREATED", saved.getId(), Map.of(
                    "appointmentId", saved.getId(),
                    "roomId", saved.getRoomId(),
                    "tenantId", tenantId,
                    "landlordId", saved.getLandlordId(),
                    "scheduledStart", saved.getScheduledStart().toString()
            ));
            return ViewingAppointmentListItemDto.fromEntity(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Ban da co lich hen xem phong nay o khung gio tuong tu", ex);
        }
    }

    @Transactional
    public ViewingAppointmentListItemDto cancelAppointment(String tenantId, String appointmentId, CancelAppointmentRequest req) {
        ViewingAppointment appointment = appointmentRepository.findByIdAndTenantId(appointmentId, tenantId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
        if (req != null && req.version() != null && !req.version().equals(appointment.getVersion())) {
            throw new OptimisticLockConflictException("Du lieu da bi thay doi. Vui long tai lai trang.");
        }
        String initialStatus = appointment.getStatus().name();
        CancelReasonCode code = req != null && req.reasonCode() != null ? req.reasonCode() : CancelReasonCode.BUSY;
        String note = req != null ? req.reasonNote() : null;
        appointment.cancel(tenantId, CancelledBy.TENANT, code, note);
        ViewingAppointment saved = appointmentRepository.save(appointment);

        historyRepository.ifPresent(repo -> repo.save(new AppointmentStatusHistory(
                saved.getId(), initialStatus, "CANCELLED", tenantId, "TENANT", note)));
        recordOutbox("APPOINTMENT_CANCELLED", saved.getId(), Map.of(
                "appointmentId", saved.getId(),
                "cancelledBy", "TENANT",
                "reasonCode", code.name()
        ));
        return ViewingAppointmentListItemDto.fromEntity(saved);
    }

    private void recordOutbox(String eventType, String aggregateId, Object payload) {
        outboxRepository.ifPresent(repo -> {
            try {
                String json = objectMapper.map(mapper -> {
                    try {
                        return mapper.writeValueAsString(payload);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).orElse("{}");
                repo.save(new AppointmentEventOutbox(eventType, aggregateId, json));
            } catch (RuntimeException ex) {
                throw new IllegalStateException("Khong the luu su kien lich hen", ex);
            }
        });
    }
}
