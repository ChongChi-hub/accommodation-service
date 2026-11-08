package vn.cozynest.booking.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.booking.application.dto.*;
import vn.cozynest.booking.application.exception.AppointmentNotFoundException;
import vn.cozynest.booking.application.exception.OptimisticLockConflictException;
import vn.cozynest.booking.application.exception.TimeConflictException;
import vn.cozynest.booking.domain.model.*;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.booking.domain.repository.AppointmentStatusHistoryRepository;
import vn.cozynest.booking.domain.repository.ViewingAppointmentRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class LandlordAppointmentService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ViewingAppointmentRepository appointmentRepository;
    private final AppointmentStatusHistoryRepository historyRepository;
    private final AppointmentEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public LandlordAppointmentService(
            ViewingAppointmentRepository appointmentRepository,
            AppointmentStatusHistoryRepository historyRepository,
            AppointmentEventOutboxRepository outboxRepository,
            ObjectMapper objectMapper
    ) {
        this.appointmentRepository = appointmentRepository;
        this.historyRepository = historyRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<ViewingAppointmentListItemDto> getAppointments(String landlordId, AppointmentFilterParams params) {
        Specification<ViewingAppointment> spec = AppointmentSpecificationBuilder.build(landlordId, params);

        int page = (params.page() != null && params.page() >= 0) ? params.page() : 0;
        int size = (params.size() != null && params.size() > 0) ? params.size() : 20;

        String sortBy = (params.sortBy() != null && !params.sortBy().isBlank()) ? params.sortBy() : "scheduledStart";
        Sort.Direction direction = "desc".equalsIgnoreCase(params.sortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return appointmentRepository.findAll(spec, pageable).map(ViewingAppointmentListItemDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public AppointmentSummaryDto getSummary(String landlordId) {
        long pending = appointmentRepository.countByLandlordIdAndStatus(landlordId, AppointmentStatus.PENDING);
        long confirmed = appointmentRepository.countByLandlordIdAndStatus(landlordId, AppointmentStatus.CONFIRMED);
        long needingUpdate = appointmentRepository.countNeedingOutcomeUpdate(landlordId, Instant.now());
        long completed = appointmentRepository.countByLandlordIdAndStatus(landlordId, AppointmentStatus.COMPLETED);
        long cancelled = appointmentRepository.countByLandlordIdAndStatus(landlordId, AppointmentStatus.CANCELLED)
                + appointmentRepository.countByLandlordIdAndStatus(landlordId, AppointmentStatus.DECLINED);
        long expired = appointmentRepository.countByLandlordIdAndStatus(landlordId, AppointmentStatus.EXPIRED);

        LocalDate today = LocalDate.now(VN_ZONE);
        Instant todayStart = today.atStartOfDay(VN_ZONE).toInstant();
        Instant tomorrowStart = today.plusDays(1).atStartOfDay(VN_ZONE).toInstant();
        long todayCount = appointmentRepository.countTodayAppointments(landlordId, todayStart, tomorrowStart);

        return new AppointmentSummaryDto(pending, confirmed, needingUpdate, completed, cancelled, expired, todayCount);
    }

    @Transactional(readOnly = true)
    public ViewingAppointmentDetailDto getDetail(String landlordId, String appointmentId) {
        ViewingAppointment appointment = findOrThrow(landlordId, appointmentId);
        List<AppointmentStatusHistoryDto> history = historyRepository.findByAppointmentIdOrderByCreatedAtAsc(appointmentId)
                .stream()
                .map(h -> new AppointmentStatusHistoryDto(
                        h.getId(), h.getAppointmentId(), h.getFromStatus(), h.getToStatus(),
                        h.getActorId(), h.getActorRole(), h.getReason(), h.getCreatedAt()))
                .toList();

        return ViewingAppointmentDetailDto.fromEntity(appointment, history);
    }

    public ViewingAppointmentDetailDto confirmAppointment(String landlordId, String appointmentId, ConfirmAppointmentRequest req) {
        ViewingAppointment appointment = findOrThrow(landlordId, appointmentId);
        checkVersion(req != null ? req.version() : null, appointment.getVersion());

        boolean force = req != null && Boolean.TRUE.equals(req.force());
        List<ViewingAppointment> overlapping = appointmentRepository.findOverlappingConfirmed(
                landlordId, appointment.getScheduledStart(), appointment.getScheduledEnd(), appointment.getId());

        if (!overlapping.isEmpty() && !force) {
            ViewingAppointment conflict = overlapping.get(0);
            throw new TimeConflictException(
                    conflict.getId(), conflict.getRoomTitle(),
                    conflict.getScheduledStart(), conflict.getScheduledEnd());
        }

        String note = req != null ? req.landlordNote() : null;
        appointment.confirm(landlordId, note);
        appointmentRepository.save(appointment);

        recordAudit(appointment.getId(), "PENDING", "CONFIRMED", landlordId, "LANDLORD", note);
        recordOutbox("APPOINTMENT_CONFIRMED", appointment.getId(), Map.of(
                "appointmentId", appointment.getId(),
                "landlordId", landlordId,
                "tenantId", appointment.getTenantId(),
                "roomId", appointment.getRoomId(),
                "forced", force
        ));

        return getDetail(landlordId, appointmentId);
    }

    public ViewingAppointmentDetailDto cancelAppointment(String landlordId, String appointmentId, CancelAppointmentRequest req) {
        ViewingAppointment appointment = findOrThrow(landlordId, appointmentId);
        checkVersion(req != null ? req.version() : null, appointment.getVersion());

        String initialStatus = appointment.getStatus().name();
        String targetStatus;

        if (appointment.getStatus() == AppointmentStatus.PENDING) {
            appointment.decline(landlordId, req.reasonCode(), req.reasonNote());
            targetStatus = "DECLINED";
        } else if (appointment.getStatus() == AppointmentStatus.CONFIRMED) {
            appointment.cancel(landlordId, CancelledBy.LANDLORD, req.reasonCode(), req.reasonNote());
            targetStatus = "CANCELLED";
        } else {
            throw new IllegalStateException("Khong the huy lich hen o trang thai " + appointment.getStatus());
        }

        appointmentRepository.save(appointment);
        recordAudit(appointment.getId(), initialStatus, targetStatus, landlordId, "LANDLORD", req.reasonNote());
        recordOutbox("APPOINTMENT_" + targetStatus, appointment.getId(), Map.of(
                "appointmentId", appointment.getId(),
                "reasonCode", req.reasonCode().name(),
                "reasonNote", req.reasonNote() != null ? req.reasonNote() : ""
        ));

        return getDetail(landlordId, appointmentId);
    }

    public ViewingAppointmentDetailDto completeAppointment(String landlordId, String appointmentId, CompleteAppointmentRequest req) {
        ViewingAppointment appointment = findOrThrow(landlordId, appointmentId);
        checkVersion(req != null ? req.version() : null, appointment.getVersion());

        appointment.complete(landlordId, req.outcome(), req.note());
        appointmentRepository.save(appointment);

        recordAudit(appointment.getId(), "CONFIRMED", req.outcome().name(), landlordId, "LANDLORD", req.note());
        recordOutbox("APPOINTMENT_" + req.outcome().name(), appointment.getId(), Map.of(
                "appointmentId", appointment.getId(),
                "outcome", req.outcome().name(),
                "note", req.note() != null ? req.note() : ""
        ));

        return getDetail(landlordId, appointmentId);
    }

    private ViewingAppointment findOrThrow(String landlordId, String appointmentId) {
        return appointmentRepository.findByIdAndLandlordId(appointmentId, landlordId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
    }

    private void checkVersion(Long requestedVersion, Long currentVersion) {
        if (requestedVersion != null && !requestedVersion.equals(currentVersion)) {
            throw new OptimisticLockConflictException(
                    "Du lieu da bi thay doi boi thao tac khac. Vui long tai lai trang.");
        }
    }

    private void recordAudit(String appointmentId, String from, String to, String actorId, String role, String reason) {
        historyRepository.save(new AppointmentStatusHistory(appointmentId, from, to, actorId, role, reason));
    }

    private void recordOutbox(String eventType, String aggregateId, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxRepository.save(new AppointmentEventOutbox(eventType, aggregateId, json));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }
}
