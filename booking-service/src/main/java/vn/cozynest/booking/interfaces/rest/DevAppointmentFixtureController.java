package vn.cozynest.booking.interfaces.rest;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.booking.application.dto.ViewingAppointmentDetailDto;
import vn.cozynest.booking.domain.model.AppointmentStatus;
import vn.cozynest.booking.domain.model.ViewingAppointment;
import vn.cozynest.booking.domain.repository.ViewingAppointmentRepository;
import vn.cozynest.booking.infrastructure.security.UserContextResolver;
import vn.cozynest.common.dto.ApiResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dev/Test-only fixture controller for E2E tests requiring a past-scheduled CONFIRMED appointment.
 * Explicitly excluded from prod/staging profiles.
 */
@RestController
@Profile({"dev", "test"})
@RequestMapping("/api/v1/dev/fixtures/appointments")
public class DevAppointmentFixtureController {

    private final ViewingAppointmentRepository appointmentRepository;
    private final UserContextResolver userContextResolver;
    private final Clock clock;

    public DevAppointmentFixtureController(
            ViewingAppointmentRepository appointmentRepository,
            UserContextResolver userContextResolver,
            Clock clock
    ) {
        this.appointmentRepository = appointmentRepository;
        this.userContextResolver = userContextResolver;
        this.clock = clock;
    }

    @PostMapping("/past-confirmed")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<ViewingAppointmentDetailDto>> createPastConfirmed(
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String landlordId = userContextResolver.getCurrentUserId();
        Instant now = clock.instant();
        Instant start = now.minus(2, ChronoUnit.HOURS);
        Instant end = now.minus(1, ChronoUnit.HOURS);

        ViewingAppointment appt = new ViewingAppointment();
        appt.setId("va-fixture-" + UUID.randomUUID().toString().substring(0, 8));
        appt.setRoomId(body != null && body.get("roomId") != null ? String.valueOf(body.get("roomId")) : "room-00000001-0000-0000-0000-000000000001");
        appt.setRoomTitle(body != null && body.get("roomTitle") != null ? String.valueOf(body.get("roomTitle")) : "Phong studio fixture E2E");
        appt.setRoomAddress("18 Ngo 86 Duy Tan, Ha Noi");
        appt.setTenantId(body != null && body.get("tenantId") != null ? String.valueOf(body.get("tenantId")) : "u-tenant-01");
        appt.setLandlordId(landlordId);
        appt.setContactName(body != null && body.get("contactName") != null ? String.valueOf(body.get("contactName")) : "Nguyen Van Fixture");
        appt.setContactPhone("0901234567");
        appt.setScheduledStart(start);
        appt.setScheduledEnd(end);
        appt.setStatus(AppointmentStatus.CONFIRMED);
        appt.setConfirmedAt(now.minus(3, ChronoUnit.HOURS));
        appt.setCreatedAt(now.minus(4, ChronoUnit.HOURS));
        appt.setUpdatedAt(now);
        appt.syncLegacyDateTimes();

        ViewingAppointment saved = appointmentRepository.save(appt);
        return ResponseEntity.ok(ApiResponse.success(ViewingAppointmentDetailDto.fromEntity(saved, List.of())));
    }
}

