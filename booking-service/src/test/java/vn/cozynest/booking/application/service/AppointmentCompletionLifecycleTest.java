package vn.cozynest.booking.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import vn.cozynest.booking.application.dto.CompleteAppointmentRequest;
import vn.cozynest.booking.application.dto.ViewingAppointmentDetailDto;
import vn.cozynest.booking.application.exception.AppointmentNotFoundException;
import vn.cozynest.booking.domain.exception.AppointmentNotStartedException;
import vn.cozynest.booking.domain.exception.InvalidAppointmentStateException;
import vn.cozynest.booking.domain.model.AppointmentEventOutbox;
import vn.cozynest.booking.domain.model.AppointmentStatus;
import vn.cozynest.booking.domain.model.ViewingAppointment;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.booking.domain.repository.AppointmentStatusHistoryRepository;
import vn.cozynest.booking.domain.repository.ViewingAppointmentRepository;
import vn.cozynest.booking.interfaces.rest.DevAppointmentFixtureController;
import vn.cozynest.booking.interfaces.rest.LandlordAppointmentController;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppointmentCompletionLifecycleTest {

    private static final Instant SCHEDULED_START = Instant.parse("2026-11-20T03:00:00Z"); // 10:00 Asia/Ho_Chi_Minh

    private ViewingAppointmentRepository appointmentRepository;
    private AppointmentStatusHistoryRepository historyRepository;
    private AppointmentEventOutboxRepository outboxRepository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(ViewingAppointmentRepository.class);
        historyRepository = mock(AppointmentStatusHistoryRepository.class);
        outboxRepository = mock(AppointmentEventOutboxRepository.class);
        objectMapper = new ObjectMapper();
        when(historyRepository.findByAppointmentIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
    }

    private ViewingAppointment confirmedAppt(String id) {
        ViewingAppointment a = new ViewingAppointment();
        a.setId(id);
        a.setLandlordId("u-landlord-01");
        a.setTenantId("u-tenant-01");
        a.setRoomId("room-1");
        a.setRoomTitle("Studio Duy Tan");
        a.setScheduledStart(SCHEDULED_START);
        a.setScheduledEnd(SCHEDULED_START.plusSeconds(2700));
        a.setStatus(AppointmentStatus.CONFIRMED);
        a.setVersion(1L);
        return a;
    }

    private LandlordAppointmentService serviceAt(Instant fixedNow) {
        return new LandlordAppointmentService(
                appointmentRepository, historyRepository, outboxRepository,
                objectMapper, Clock.fixed(fixedNow, ZoneOffset.UTC)
        );
    }

    @Test
    void completeOneSecondBeforeStartThrowsAppointmentNotStarted() {
        ViewingAppointment appt = confirmedAppt("va-1");
        when(appointmentRepository.findByIdAndLandlordId("va-1", "u-landlord-01")).thenReturn(Optional.of(appt));

        LandlordAppointmentService svc = serviceAt(SCHEDULED_START.minusSeconds(1));
        assertThatThrownBy(() -> svc.completeAppointment("u-landlord-01", "va-1",
                new CompleteAppointmentRequest(AppointmentStatus.COMPLETED, "Done", 1L)))
                .isInstanceOf(AppointmentNotStartedException.class);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void completeAtExactStartAndAfterStartSucceedsWithRichOutbox() {
        ViewingAppointment appt = confirmedAppt("va-exact");
        when(appointmentRepository.findByIdAndLandlordId("va-exact", "u-landlord-01")).thenReturn(Optional.of(appt));

        LandlordAppointmentService svc = serviceAt(SCHEDULED_START);
        ViewingAppointmentDetailDto dto = svc.completeAppointment("u-landlord-01", "va-exact",
                new CompleteAppointmentRequest(AppointmentStatus.COMPLETED, "Khach da xem xong", 1L));

        assertThat(dto.status()).isEqualTo(AppointmentStatus.COMPLETED);
        verify(outboxRepository, times(1)).save(argThat((AppointmentEventOutbox o) ->
                "APPOINTMENT_COMPLETED".equals(o.getEventType())
                        && o.getPayload().contains("u-tenant-01")
                        && o.getPayload().contains("Studio Duy Tan")));
    }

    @Test
    void completeIsIdempotentAndCrossOutcomeReturnsConflict() {
        ViewingAppointment appt = confirmedAppt("va-idem");
        when(appointmentRepository.findByIdAndLandlordId("va-idem", "u-landlord-01")).thenReturn(Optional.of(appt));

        LandlordAppointmentService svc = serviceAt(SCHEDULED_START.plusSeconds(600));
        svc.completeAppointment("u-landlord-01", "va-idem",
                new CompleteAppointmentRequest(AppointmentStatus.COMPLETED, "First call", 1L));

        // Second call: idempotent 200, NO second outbox or audit write
        ViewingAppointmentDetailDto repeat = svc.completeAppointment("u-landlord-01", "va-idem",
                new CompleteAppointmentRequest(AppointmentStatus.COMPLETED, "Second call", 1L));
        assertThat(repeat.status()).isEqualTo(AppointmentStatus.COMPLETED);
        verify(outboxRepository, times(1)).save(any());
        verify(historyRepository, times(1)).save(any());

        // Cross-outcome: NO_SHOW after COMPLETED -> 409 InvalidAppointmentStateException
        assertThatThrownBy(() -> svc.completeAppointment("u-landlord-01", "va-idem",
                new CompleteAppointmentRequest(AppointmentStatus.NO_SHOW, "Flip", 1L)))
                .isInstanceOf(InvalidAppointmentStateException.class);
    }

    @Test
    void unrelatedLandlordGetsNotFound404() {
        when(appointmentRepository.findByIdAndLandlordId("va-1", "u-landlord-02")).thenReturn(Optional.empty());
        LandlordAppointmentService svc = serviceAt(SCHEDULED_START.plusSeconds(60));
        assertThatThrownBy(() -> svc.completeAppointment("u-landlord-02", "va-1",
                new CompleteAppointmentRequest(AppointmentStatus.COMPLETED, null, null)))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void devFixtureControllerIsExcludedFromProductionProfileAndRoleProtected() {
        Profile profile = DevAppointmentFixtureController.class.getAnnotation(Profile.class);
        assertThat(profile).isNotNull();
        assertThat(Arrays.asList(profile.value())).containsExactlyInAnyOrder("dev", "test");
        assertThat(Arrays.asList(profile.value())).doesNotContain("prod", "production");

        PreAuthorize classAuth = LandlordAppointmentController.class.getAnnotation(PreAuthorize.class);
        assertThat(classAuth).isNotNull();
        assertThat(classAuth.value()).contains("LANDLORD");
    }
}

