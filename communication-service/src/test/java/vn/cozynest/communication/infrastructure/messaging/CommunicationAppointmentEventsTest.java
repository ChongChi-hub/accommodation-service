package vn.cozynest.communication.infrastructure.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.cozynest.common.events.DomainEventEnvelope;
import vn.cozynest.common.events.EventEnvelopeCodec;
import vn.cozynest.common.events.EventInboxProcessor;
import vn.cozynest.communication.application.service.NotificationApplicationService;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommunicationAppointmentEventsTest {

    private CommunicationInboxStore inboxStore;
    private NotificationApplicationService notificationService;
    private CommunicationEventListener listener;
    private Set<String> processedIds;

    @BeforeEach
    void setUp() {
        inboxStore = mock(CommunicationInboxStore.class);
        notificationService = mock(NotificationApplicationService.class);
        processedIds = new HashSet<>();
        when(inboxStore.existsByEventId(anyString())).thenAnswer(inv -> processedIds.contains(inv.getArgument(0)));
        doAnswer(inv -> {
            DomainEventEnvelope env = inv.getArgument(0);
            processedIds.add(env.eventId());
            return null;
        }).when(inboxStore).recordProcessed(any());
        listener = new CommunicationEventListener(inboxStore, notificationService, Optional.empty(), Optional.empty());
    }

    @Test
    void appointmentCompletedAndNoShowCreateTenantNotificationIdempotently() {
        DomainEventEnvelope completedEnv = new DomainEventEnvelope(
                "evt-appt-comp-1", "APPOINTMENT_COMPLETED", 1, "APPOINTMENT",
                "va-100", "booking-service", Instant.now(),
                "{\"appointmentId\":\"va-100\",\"tenantId\":\"u-tenant-01\",\"landlordId\":\"u-landlord-01\",\"roomTitle\":\"Studio Duy Tan\",\"outcome\":\"COMPLETED\"}"
        );
        String json = EventEnvelopeCodec.encode(completedEnv);

        assertThat(listener.handleRawMessage(json)).isEqualTo(EventInboxProcessor.ProcessOutcome.PROCESSED);
        assertThat(listener.handleRawMessage(json)).isEqualTo(EventInboxProcessor.ProcessOutcome.DUPLICATE_IGNORED);

        verify(notificationService, times(1)).createInternalNotification(
                eq("u-tenant-01"), eq("APPOINTMENT"),
                contains("hoàn thành"), contains("Studio Duy Tan"), eq("/dashboard/appointments")
        );

        DomainEventEnvelope noShowEnv = new DomainEventEnvelope(
                "evt-appt-noshow-1", "APPOINTMENT_NO_SHOW", 1, "APPOINTMENT",
                "va-101", "booking-service", Instant.now(),
                "{\"appointmentId\":\"va-101\",\"tenantId\":\"u-tenant-01\",\"landlordId\":\"u-landlord-01\",\"roomTitle\":\"Studio Cau Giay\",\"outcome\":\"NO_SHOW\"}"
        );
        assertThat(listener.handleRawMessage(EventEnvelopeCodec.encode(noShowEnv)))
                .isEqualTo(EventInboxProcessor.ProcessOutcome.PROCESSED);
        verify(notificationService, times(1)).createInternalNotification(
                eq("u-tenant-01"), eq("APPOINTMENT"),
                contains("không đến"), contains("Studio Cau Giay"), eq("/dashboard/appointments")
        );
    }

    @Test
    void rentalRequestRejectedIncludesRejectionReasonInNotification() {
        DomainEventEnvelope rejectedEnv = new DomainEventEnvelope(
                "evt-rr-rej-1", "RENTAL_REQUEST_REJECTED", 1, "RENTAL_REQUEST",
                "req-200", "booking-service", Instant.now(),
                "{\"requestId\":\"req-200\",\"tenantId\":\"u-tenant-01\",\"landlordId\":\"u-landlord-01\",\"roomTitle\":\"Studio Cau Giay\",\"rejectionReason\":\"Phong da co nguoi dat coc\"}"
        );
        assertThat(listener.handleRawMessage(EventEnvelopeCodec.encode(rejectedEnv)))
                .isEqualTo(EventInboxProcessor.ProcessOutcome.PROCESSED);
        verify(notificationService, times(1)).createInternalNotification(
                eq("u-tenant-01"), eq("RENTAL_REQUEST"),
                contains("từ chối"), contains("Phong da co nguoi dat coc"), eq("/dashboard/rental-requests")
        );
    }
}
