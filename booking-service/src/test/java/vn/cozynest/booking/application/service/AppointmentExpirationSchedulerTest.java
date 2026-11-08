package vn.cozynest.booking.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.cozynest.booking.domain.model.AppointmentStatus;
import vn.cozynest.booking.domain.model.ViewingAppointment;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.booking.domain.repository.AppointmentStatusHistoryRepository;
import vn.cozynest.booking.domain.repository.ViewingAppointmentRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private Clock fixedClock;
    private AppointmentExpirationScheduler scheduler;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-11-08T10:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        scheduler = new AppointmentExpirationScheduler(
                appointmentRepository,
                historyRepository,
                outboxRepository,
                objectMapper,
                fixedClock,
                48
        );
    }

    @Test
    @DisplayName("expirePendingPastAppointments should expire appointments and record outbox event idempotently")
    void testExpirePendingPastAppointments() {
        ViewingAppointment pastAppt = new ViewingAppointment();
        pastAppt.setId("expired-01");
        pastAppt.setLandlordId("landlord-01");
        pastAppt.setTenantId("tenant-01");
        pastAppt.setStatus(AppointmentStatus.PENDING);
        pastAppt.setScheduledStart(fixedClock.instant().minus(2, ChronoUnit.HOURS));

        when(appointmentRepository.findExpiredPending(any(), any())).thenReturn(List.of(pastAppt));
        when(appointmentRepository.atomicallyExpirePending(eq("expired-01"), any())).thenReturn(1);

        scheduler.expirePendingPastAppointments();

        verify(appointmentRepository).atomicallyExpirePending(eq("expired-01"), eq(fixedClock.instant()));
        verify(historyRepository, times(1)).save(any());
        verify(outboxRepository, times(1)).save(any());

        // Second run with 0 affected (already expired / idempotent)
        when(appointmentRepository.atomicallyExpirePending(eq("expired-01"), any())).thenReturn(0);
        scheduler.expirePendingPastAppointments();

        verify(historyRepository, times(1)).save(any());
        verify(outboxRepository, times(1)).save(any());
    }
}
