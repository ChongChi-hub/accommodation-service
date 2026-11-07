package vn.toam.booking.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.toam.booking.application.dto.CancelAppointmentRequest;
import vn.toam.booking.application.dto.ConfirmAppointmentRequest;
import vn.toam.booking.application.dto.ViewingAppointmentDetailDto;
import vn.toam.booking.application.exception.AppointmentNotFoundException;
import vn.toam.booking.application.exception.OptimisticLockConflictException;
import vn.toam.booking.application.exception.TimeConflictException;
import vn.toam.booking.domain.model.AppointmentStatus;
import vn.toam.booking.domain.model.CancelReasonCode;
import vn.toam.booking.domain.model.ViewingAppointment;
import vn.toam.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.toam.booking.domain.repository.AppointmentStatusHistoryRepository;
import vn.toam.booking.domain.repository.ViewingAppointmentRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LandlordAppointmentServiceTest {

    @Mock
    private ViewingAppointmentRepository appointmentRepository;
    @Mock
    private AppointmentStatusHistoryRepository historyRepository;
    @Mock
    private AppointmentEventOutboxRepository outboxRepository;

    private LandlordAppointmentService service;

    @BeforeEach
    void setUp() {
        service = new LandlordAppointmentService(
                appointmentRepository, historyRepository, outboxRepository, new ObjectMapper());
    }

    private ViewingAppointment createSample(String id, String landlordId, AppointmentStatus status) {
        ViewingAppointment appt = new ViewingAppointment();
        appt.setId(id);
        appt.setLandlordId(landlordId);
        appt.setTenantId("tenant-01");
        appt.setRoomId("room-01");
        appt.setStatus(status);
        appt.setContactPhone("0901234567");
        appt.setScheduledStart(Instant.now().plus(1, ChronoUnit.DAYS));
        appt.setScheduledEnd(Instant.now().plus(1, ChronoUnit.DAYS).plus(45, ChronoUnit.MINUTES));
        appt.setVersion(0L);
        return appt;
    }

    @Test
    @DisplayName("getDetail should mask phone when status is PENDING and unmask when CONFIRMED")
    void testPhoneMaskingInDetail() {
        ViewingAppointment pending = createSample("appt-01", "landlord-01", AppointmentStatus.PENDING);
        when(appointmentRepository.findByIdAndLandlordId("appt-01", "landlord-01")).thenReturn(Optional.of(pending));
        when(historyRepository.findByAppointmentIdOrderByCreatedAtAsc("appt-01")).thenReturn(List.of());

        ViewingAppointmentDetailDto detail = service.getDetail("landlord-01", "appt-01");
        assertThat(detail.contactPhone()).isEqualTo("090••••567");

        ViewingAppointment confirmed = createSample("appt-02", "landlord-01", AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findByIdAndLandlordId("appt-02", "landlord-01")).thenReturn(Optional.of(confirmed));
        when(historyRepository.findByAppointmentIdOrderByCreatedAtAsc("appt-02")).thenReturn(List.of());

        ViewingAppointmentDetailDto detailConfirmed = service.getDetail("landlord-01", "appt-02");
        assertThat(detailConfirmed.contactPhone()).isEqualTo("0901234567");
    }

    @Test
    @DisplayName("confirmAppointment without force throws TimeConflictException when overlapping")
    void testConfirmConflictWithoutForce() {
        ViewingAppointment target = createSample("appt-target", "landlord-01", AppointmentStatus.PENDING);
        ViewingAppointment conflicting = createSample("appt-existing", "landlord-01", AppointmentStatus.CONFIRMED);
        conflicting.setRoomTitle("Phong 101");

        when(appointmentRepository.findByIdAndLandlordId("appt-target", "landlord-01")).thenReturn(Optional.of(target));
        when(appointmentRepository.findOverlappingConfirmed(eq("landlord-01"), any(), any(), eq("appt-target")))
                .thenReturn(List.of(conflicting));

        ConfirmAppointmentRequest req = new ConfirmAppointmentRequest(false, "Note", 0L);

        assertThatThrownBy(() -> service.confirmAppointment("landlord-01", "appt-target", req))
                .isInstanceOf(TimeConflictException.class);
    }

    @Test
    @DisplayName("confirmAppointment with force=true succeeds even if overlapping")
    void testConfirmConflictWithForce() {
        ViewingAppointment target = createSample("appt-target", "landlord-01", AppointmentStatus.PENDING);
        ViewingAppointment conflicting = createSample("appt-existing", "landlord-01", AppointmentStatus.CONFIRMED);

        when(appointmentRepository.findByIdAndLandlordId("appt-target", "landlord-01")).thenReturn(Optional.of(target));
        when(appointmentRepository.findOverlappingConfirmed(eq("landlord-01"), any(), any(), eq("appt-target")))
                .thenReturn(List.of(conflicting));
        when(historyRepository.findByAppointmentIdOrderByCreatedAtAsc("appt-target")).thenReturn(List.of());

        ConfirmAppointmentRequest req = new ConfirmAppointmentRequest(true, "Force confirm note", 0L);
        ViewingAppointmentDetailDto result = service.confirmAppointment("landlord-01", "appt-target", req);

        assertThat(result.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(outboxRepository).save(any());
        verify(historyRepository).save(any());
    }

    @Test
    @DisplayName("confirmAppointment throws OptimisticLockConflictException if version mismatches")
    void testVersionConflict() {
        ViewingAppointment target = createSample("appt-target", "landlord-01", AppointmentStatus.PENDING);
        target.setVersion(2L);
        when(appointmentRepository.findByIdAndLandlordId("appt-target", "landlord-01")).thenReturn(Optional.of(target));

        ConfirmAppointmentRequest req = new ConfirmAppointmentRequest(false, null, 1L);

        assertThatThrownBy(() -> service.confirmAppointment("landlord-01", "appt-target", req))
                .isInstanceOf(OptimisticLockConflictException.class);
    }

    @Test
    @DisplayName("accessing appointment belonging to another landlord throws AppointmentNotFoundException (IDOR)")
    void testIdorProtection() {
        when(appointmentRepository.findByIdAndLandlordId("appt-01", "landlord-02")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail("landlord-02", "appt-01"))
                .isInstanceOf(AppointmentNotFoundException.class);
    }
}
