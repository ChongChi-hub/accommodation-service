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
    private final Optional<vn.cozynest.booking.application.port.IdentityVerificationPort> identityVerificationPort;

    public TenantAppointmentService(ViewingAppointmentRepository appointmentRepository) {
        this(appointmentRepository, null, null, null, null);
    }

    public TenantAppointmentService(
            ViewingAppointmentRepository appointmentRepository,
            AppointmentStatusHistoryRepository historyRepository,
            AppointmentEventOutboxRepository outboxRepository,
            ObjectMapper objectMapper) {
        this(appointmentRepository, historyRepository, outboxRepository, objectMapper, null);
    }

    @Autowired
    public TenantAppointmentService(
            ViewingAppointmentRepository appointmentRepository,
            AppointmentStatusHistoryRepository historyRepository,
            AppointmentEventOutboxRepository outboxRepository,
            ObjectMapper objectMapper,
            @Autowired(required = false) vn.cozynest.booking.application.port.IdentityVerificationPort identityVerificationPort
    ) {
        this.appointmentRepository = appointmentRepository;
        this.historyRepository = Optional.ofNullable(historyRepository);
        this.outboxRepository = Optional.ofNullable(outboxRepository);
        this.objectMapper = Optional.ofNullable(objectMapper);
        this.identityVerificationPort = Optional.ofNullable(identityVerificationPort);
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
        identityVerificationPort.ifPresent(port -> {
            vn.cozynest.booking.application.port.IdentityVerificationPort.UserVerificationStatus status = port.getVerificationStatus(tenantId);
            if (status != null && !status.emailVerified()) {
                throw new vn.cozynest.booking.application.exception.VerificationRequirementException(
                        "Vui lòng xác minh địa chỉ email trước khi đặt lịch hẹn xem phòng.", "EMAIL_NOT_VERIFIED"
                );
            }
        });

        Instant now = Instant.now();
        if (req.scheduledStart().isBefore(now.minusSeconds(60))) {
            throw new IllegalArgumentException("Thoi gian dat lich xem phong phai o tuong lai");
        }
        Instant end = req.scheduledEnd() != null ? req.scheduledEnd() : req.scheduledStart().plusSeconds(45L * 60L);
        if (!end.isAfter(req.scheduledStart())) {
            throw new IllegalArgumentException("Thoi gian ket thuc phai sau thoi gian bat dau");
        }
        String targetLandlordId = normalizeLandlordId(req.landlordId());
        if (LandlordIdentityResolver.matches(tenantId, targetLandlordId)) {
            throw new IllegalArgumentException("Chu tro khong the tu dat lich xem phong cua chinh minh");
        }

        ViewingAppointment appointment = new ViewingAppointment();
        appointment.setId(UUID.randomUUID().toString());
        appointment.setRoomId(req.roomId().trim());
        appointment.setTenantId(tenantId);
        appointment.setLandlordId(targetLandlordId);
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
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("appointmentId", saved.getId());
            payload.put("roomId", saved.getRoomId());
            payload.put("tenantId", tenantId);
            payload.put("landlordId", saved.getLandlordId());
            payload.put("scheduledStart", saved.getScheduledStart().toString());
            if (saved.getScheduledEnd() != null) payload.put("scheduledEnd", saved.getScheduledEnd().toString());
            if (saved.getRoomTitle() != null) payload.put("roomTitle", saved.getRoomTitle());
            if (saved.getRoomAddress() != null) payload.put("roomAddress", saved.getRoomAddress());
            if (saved.getContactName() != null) payload.put("contactName", saved.getContactName());
            if (saved.getContactPhone() != null) payload.put("contactPhone", saved.getContactPhone());
            if (saved.getContactEmail() != null) payload.put("contactEmail", saved.getContactEmail());
            if (saved.getTenantNote() != null) payload.put("tenantNote", saved.getTenantNote());
            recordOutbox("APPOINTMENT_CREATED", saved.getId(), payload);
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
        Map<String, Object> cancelPayload = new java.util.HashMap<>();
        cancelPayload.put("appointmentId", saved.getId());
        cancelPayload.put("roomId", saved.getRoomId());
        cancelPayload.put("tenantId", tenantId);
        cancelPayload.put("landlordId", saved.getLandlordId());
        cancelPayload.put("cancelledBy", "TENANT");
        cancelPayload.put("reasonCode", code.name());
        cancelPayload.put("reasonNote", note != null ? note : "");
        if (saved.getRoomTitle() != null) cancelPayload.put("roomTitle", saved.getRoomTitle());
        if (saved.getRoomAddress() != null) cancelPayload.put("roomAddress", saved.getRoomAddress());
        if (saved.getScheduledStart() != null) cancelPayload.put("scheduledStart", saved.getScheduledStart().toString());
        if (saved.getContactName() != null) cancelPayload.put("contactName", saved.getContactName());
        if (saved.getContactPhone() != null) cancelPayload.put("contactPhone", saved.getContactPhone());
        if (saved.getContactEmail() != null) cancelPayload.put("contactEmail", saved.getContactEmail());
        recordOutbox("APPOINTMENT_CANCELLED", saved.getId(), cancelPayload);
        return ViewingAppointmentListItemDto.fromEntity(saved);
    }

    private String normalizeLandlordId(String landlordId) {
        if (landlordId == null) return null;
        String legacy = LandlordIdentityResolver.normalizeLegacy(landlordId);
        if (legacy != null && legacy.matches("^\\d+$")) {
            return identityVerificationPort
                    .map(port -> port.resolveLandlordUserId(legacy))
                    .orElse(legacy);
        }
        return legacy;
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
