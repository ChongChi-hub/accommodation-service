package vn.cozynest.communication.infrastructure.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import vn.cozynest.common.events.DomainEventEnvelope;
import vn.cozynest.common.events.EventEnvelopeCodec;
import vn.cozynest.common.events.EventInboxProcessor;
import vn.cozynest.common.events.EventTopology;
import vn.cozynest.communication.application.service.NotificationApplicationService;
import vn.cozynest.communication.infrastructure.persistence.EventInboxJpaEntity;
import vn.cozynest.communication.infrastructure.persistence.SpringDataEventInboxRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CommunicationEventListenerDoDTest {

    @Test
    @DisplayName("DoD: Tắt communication-service lúc tạo hợp đồng/hóa đơn -> bật lại toàn bộ event trong hàng đợi vẫn được xử lý đủ, không mất thông báo")
    void verifiesOfflineEventsProcessedOnReconnectionWithoutMessageLoss() {
        // Setup in-memory store representing inbox_messages
        SpringDataEventInboxRepository inboxRepo = mock(SpringDataEventInboxRepository.class);
        ConcurrentHashMap<String, EventInboxJpaEntity> inboxStoreMap = new ConcurrentHashMap<>();

        when(inboxRepo.existsById(anyString())).thenAnswer(inv -> inboxStoreMap.containsKey(inv.getArgument(0)));
        when(inboxRepo.save(any(EventInboxJpaEntity.class))).thenAnswer(inv -> {
            EventInboxJpaEntity entity = inv.getArgument(0);
            inboxStoreMap.put(entity.getEventId(), entity);
            return entity;
        });
        when(inboxRepo.findMaxOccurredAt(anyString(), anyString(), anyString())).thenAnswer(inv -> {
            String aggType = inv.getArgument(0);
            String aggId = inv.getArgument(1);
            String evtType = inv.getArgument(2);
            return inboxStoreMap.values().stream()
                    .filter(e -> aggType.equals(e.getEventId()))
                    .map(EventInboxJpaEntity::getOccurredAt)
                    .max(Instant::compareTo);
        });

        NotificationApplicationService notifService = mock(NotificationApplicationService.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

        CommunicationInboxStore inboxStore = new CommunicationInboxStore(inboxRepo, Optional.of(rabbitTemplate));
        CommunicationEventListener listener = new CommunicationEventListener(inboxStore, notifService, Optional.of(rabbitTemplate));

        // 1. Contract Created Event queued while communication was down
        DomainEventEnvelope contractEvent = new DomainEventEnvelope(
                "evt-contract-001",
                "CONTRACT_CREATED",
                1,
                "CONTRACT",
                "ctr-12345",
                "corr-ctr-001",
                Instant.now(),
                "{\"contractId\":\"ctr-12345\",\"contractCode\":\"HD-2026-001\",\"roomId\":\"room-88\",\"tenantId\":\"usr-tenant-1\",\"landlordId\":\"usr-landlord-1\",\"status\":\"ACTIVE\"}"
        );

        // 2. Invoice Generated Event queued while communication was down
        DomainEventEnvelope invoiceEvent = new DomainEventEnvelope(
                "evt-invoice-001",
                "INVOICE_GENERATED",
                1,
                "INVOICE",
                "inv-98765",
                "corr-inv-001",
                Instant.now(),
                "{\"invoiceId\":\"inv-98765\",\"invoiceCode\":\"INV-2026-001\",\"contractId\":\"ctr-12345\",\"tenantId\":\"usr-tenant-1\",\"totalAmount\":4500000,\"status\":\"UNPAID\"}"
        );

        // Turn on communication service -> processes queued events
        String contractJson = EventEnvelopeCodec.encode(contractEvent);
        String invoiceJson = EventEnvelopeCodec.encode(invoiceEvent);

        EventInboxProcessor.ProcessOutcome contractOutcome = listener.handleRawMessage(contractJson);
        EventInboxProcessor.ProcessOutcome invoiceOutcome = listener.handleRawMessage(invoiceJson);

        assertThat(contractOutcome).isEqualTo(EventInboxProcessor.ProcessOutcome.PROCESSED);
        assertThat(invoiceOutcome).isEqualTo(EventInboxProcessor.ProcessOutcome.PROCESSED);

        // Notifications created for both tenant and landlord on contract creation
        verify(notifService).createInternalNotification(
                eq("usr-tenant-1"), eq("CONTRACT"),
                eq("Hợp đồng thuê phòng mới đã được tạo"),
                eq("Mã hợp đồng: HD-2026-001"),
                eq("/dashboard/contracts")
        );
        verify(notifService).createInternalNotification(
                eq("usr-landlord-1"), eq("CONTRACT"),
                eq("Hợp đồng thuê phòng mới đã được tạo"),
                eq("Mã hợp đồng: HD-2026-001"),
                eq("/dashboard/contracts")
        );

        // Notification created for tenant on invoice generated
        verify(notifService).createInternalNotification(
                eq("usr-tenant-1"), eq("INVOICE"),
                eq("Hóa đơn thuê phòng kỳ mới đã phát hành"),
                eq("Mã hóa đơn: INV-2026-001"),
                eq("/dashboard/invoices")
        );

        // Check inbox_messages entries exist
        assertThat(inboxStoreMap).containsKey("evt-contract-001");
        assertThat(inboxStoreMap).containsKey("evt-invoice-001");

        // 3. Idempotent Redelivery: Same events re-delivered must be IGNORED without extra notifications
        clearInvocations(notifService);
        assertThat(listener.handleRawMessage(contractJson)).isEqualTo(EventInboxProcessor.ProcessOutcome.DUPLICATE_IGNORED);
        assertThat(listener.handleRawMessage(invoiceJson)).isEqualTo(EventInboxProcessor.ProcessOutcome.DUPLICATE_IGNORED);
        verifyNoInteractions(notifService);

        // 4. Stale Out-of-Order Event: Older event for already recorded aggregate routed to DLQ
        when(inboxRepo.findMaxOccurredAt(eq("CONTRACT"), eq("ctr-12345"), eq("CONTRACT_ACTIVATED")))
                .thenReturn(Optional.of(Instant.now().plusSeconds(100)));

        DomainEventEnvelope staleEvent = new DomainEventEnvelope(
                "evt-stale-001",
                "CONTRACT_ACTIVATED",
                1,
                "CONTRACT",
                "ctr-12345",
                "corr-stale-001",
                Instant.now().minusSeconds(50),
                "{\"contractId\":\"ctr-12345\",\"tenantId\":\"usr-tenant-1\"}"
        );
        assertThat(listener.handleRawMessage(EventEnvelopeCodec.encode(staleEvent)))
                .isEqualTo(EventInboxProcessor.ProcessOutcome.STALE_OUT_OF_ORDER);

        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                eq(EventTopology.DLX_EXCHANGE),
                eq(EventTopology.DLQ_ROUTING_KEY),
                anyString()
        );
    }
}
