package vn.cozynest.booking.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import vn.cozynest.booking.application.dto.CancelAppointmentRequest;
import vn.cozynest.booking.application.dto.CreateAppointmentRequest;
import vn.cozynest.booking.application.dto.ViewingAppointmentListItemDto;
import vn.cozynest.booking.domain.model.AppointmentStatus;
import vn.cozynest.booking.domain.model.CancelReasonCode;
import vn.cozynest.booking.domain.model.ViewingAppointment;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.booking.domain.repository.AppointmentStatusHistoryRepository;
import vn.cozynest.booking.domain.repository.ViewingAppointmentRepository;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TenantAppointmentServiceTest {

    @Test
    void createAndCancelAppointmentPersistsHistoryAndOutbox() {
        ViewingAppointmentRepository repo = mock(ViewingAppointmentRepository.class);
        AppointmentStatusHistoryRepository historyRepo = mock(AppointmentStatusHistoryRepository.class);
        AppointmentEventOutboxRepository outboxRepo = mock(AppointmentEventOutboxRepository.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TenantAppointmentService service = new TenantAppointmentService(repo, historyRepo, outboxRepo, new ObjectMapper());
        Instant start = Instant.now().plusSeconds(7200);
        ViewingAppointmentListItemDto created = service.createAppointment(
                "tenant-1",
                new CreateAppointmentRequest("101", "landlord-1", start, null, "Muon xem buoi chieu", "Phong A", "Q1", null, "Nguyen Van A", "0909123456", "a@cozynest.vn")
        );

        assertEquals(AppointmentStatus.PENDING, created.status());
        assertEquals("101", created.roomId());
        assertNotEquals("0909123456", created.contactPhone()); // masked while PENDING
        verify(historyRepo, times(1)).save(any());
        verify(outboxRepo, times(1)).save(any());

        ViewingAppointment savedEntity = new ViewingAppointment();
        savedEntity.setId(created.id());
        savedEntity.setTenantId("tenant-1");
        savedEntity.setLandlordId("landlord-1");
        savedEntity.setRoomId("101");
        savedEntity.setStatus(AppointmentStatus.PENDING);
        savedEntity.setScheduledStart(start);
        savedEntity.setScheduledEnd(start.plusSeconds(2700));
        when(repo.findByIdAndTenantId(created.id(), "tenant-1")).thenReturn(Optional.of(savedEntity));

        ViewingAppointmentListItemDto cancelled = service.cancelAppointment(
                "tenant-1",
                created.id(),
                new CancelAppointmentRequest(CancelReasonCode.BUSY, "Ban viec dot xuat", 0L)
        );
        assertEquals(AppointmentStatus.CANCELLED, cancelled.status());
        verify(historyRepo, times(2)).save(any());
        verify(outboxRepo, times(2)).save(any());
    }

    @Test
    void createAppointmentRejectsPastStartTimeAndSelfBooking() {
        ViewingAppointmentRepository repo = mock(ViewingAppointmentRepository.class);
        TenantAppointmentService service = new TenantAppointmentService(repo);

        assertThrows(IllegalArgumentException.class, () -> service.createAppointment(
                "tenant-1",
                new CreateAppointmentRequest("101", "landlord-1", Instant.now().minusSeconds(600), null, null, null, null, null, null, null, null)
        ));

        assertThrows(IllegalArgumentException.class, () -> service.createAppointment(
                "landlord-1",
                new CreateAppointmentRequest("101", "landlord-1", Instant.now().plusSeconds(3600), null, null, null, null, null, null, null, null)
        ));
    }
}

