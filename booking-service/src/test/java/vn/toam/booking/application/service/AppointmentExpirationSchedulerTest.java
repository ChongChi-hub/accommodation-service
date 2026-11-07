package vn.toam.booking.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.toam.booking.domain.model.AppointmentStatus;
import vn.toam.booking.domain.model.ViewingAppointment;
import vn.toam.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.toam.booking.domain.repository.AppointmentStatusHistoryRepository;
import vn.toam.booking.domain.repository.ViewingAppointmentRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentExpirationSchedulerTest {

    @Mock
    private ViewingAppointmentRepository appointmentRepository;
    @Mock
    private AppointmentStatusHistoryRepository historyRepository;
    @Mock
    private AppointmentEventOutboxRepository outboxRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AppointmentExpirationScheduler scheduler;

    @Test
    @DisplayName("expirePendingPastAppointments should expire past pending appointments and record outbox event")
    void testExpirePendingPastAppointments() {
        ViewingAppointment pastAppt = new ViewingAppointment();
        pastAppt.setId("expired-01");
        pastAppt.setLandlordId("landlord-01");
        pastAppt.setTenantId("tenant-01");
        pastAppt.setStatus(AppointmentStatus.PENDING);
        pastAppt.setScheduledStart(Instant.now().minus(2, ChronoUnit.HOURS));

        when(appointmentRepository.findExpiredPending(any())).thenReturn(List.of(pastAppt));

        scheduler.expirePendingPastAppointments();

        assertThat(pastAppt.getStatus()).isEqualTo(AppointmentStatus.EXPIRED);
        verify(appointmentRepository).save(pastAppt);
        verify(historyRepository).save(any());
        verify(outboxRepository).save(any());
    }
}
