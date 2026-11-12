package vn.cozynest.communication.infrastructure.messaging;

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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CommunicationBrokerDlqTest {

    @Test
    void processesValidEventIdempotentlyAndRoutesPoisonOrUnsupportedSchemaToDlq() {
        SpringDataEventInboxRepository inboxRepo = mock(SpringDataEventInboxRepository.class);
        ConcurrentHashMap<String, EventInboxJpaEntity> stored = new ConcurrentHashMap<>();
        when(inboxRepo.existsById(anyString())).thenAnswer(inv -> stored.containsKey(inv.getArgument(0)));
        when(inboxRepo.save(any())).thenAnswer(inv -> {
            EventInboxJpaEntity e = inv.getArgument(0);
            stored.put(e.getEventId(), e);
            return e;
        });
        when(inboxRepo.findMaxOccurredAt(anyString(), anyString(), anyString())).thenReturn(Optional.empty());

        NotificationApplicationService notifService = mock(NotificationApplicationService.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        CommunicationInboxStore inboxStore = new CommunicationInboxStore(inboxRepo, Optional.of(rabbitTemplate));
        CommunicationEventListener listener = new CommunicationEventListener(inboxStore, notifService, Optional.of(rabbitTemplate));

        DomainEventEnvelope valid = new DomainEventEnvelope(
                "evt-100", "PAYMENT_CONFIRMED", 1, "PAYMENT", "ord-1", "corr-1",
                Instant.now(), "{\"tenantId\":\"u-tenant-01\",\"message\":\"Coc thanh cong\"}"
        );
        String raw = EventEnvelopeCodec.encode(valid);

        assertThat(listener.handleRawMessage(raw)).isEqualTo(EventInboxProcessor.ProcessOutcome.PROCESSED);
        // Duplicate redelivery must be ignored idempotently without creating second notification
        assertThat(listener.handleRawMessage(raw)).isEqualTo(EventInboxProcessor.ProcessOutcome.DUPLICATE_IGNORED);
        verify(notifService, times(1)).createInternalNotification(eq("u-tenant-01"), eq("PAYMENT"), anyString(), eq("Coc thanh cong"), anyString());

        // Unsupported schema version > 1 must be quarantined to DLX/DLQ
        DomainEventEnvelope futureSchema = new DomainEventEnvelope(
                "evt-101", "PAYMENT_CONFIRMED", 99, "PAYMENT", "ord-1", "corr-2",
                Instant.now(), "{\"tenantId\":\"u-tenant-01\",\"phone\":\"0901234567\"}"
        );
        assertThat(listener.handleRawMessage(EventEnvelopeCodec.encode(futureSchema)))
                .isEqualTo(EventInboxProcessor.ProcessOutcome.SENT_TO_DLQ);

        // Poison malformed JSON payload quarantined with redacted secrets
        assertThat(listener.handleRawMessage("{\"password\":\"secret123\", INVALID"))
                .isEqualTo(EventInboxProcessor.ProcessOutcome.SENT_TO_DLQ);

        verify(rabbitTemplate, atLeast(2)).convertAndSend(
                eq(EventTopology.DLX_EXCHANGE),
                eq(EventTopology.DLQ_ROUTING_KEY),
                argThat((String payload) -> !payload.contains("0901234567") && !payload.contains("secret123"))
        );
    }
}

