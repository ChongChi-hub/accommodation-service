package vn.cozynest.booking.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.booking.application.port.NotificationDispatchPort;
import vn.cozynest.booking.domain.model.AppointmentEventOutbox;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.common.events.DomainEventEnvelope;
import vn.cozynest.common.events.EventEnvelopeCodec;
import vn.cozynest.common.events.EventTopology;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentOutboxPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentOutboxPublisherScheduler.class);

    private final AppointmentEventOutboxRepository outboxRepository;
    private final NotificationDispatchPort notificationDispatchPort;
    private final Optional<RabbitTemplate> rabbitTemplate;
    private final boolean brokerEnabled;

    public AppointmentOutboxPublisherScheduler(
            AppointmentEventOutboxRepository outboxRepository,
            NotificationDispatchPort notificationDispatchPort) {
        this(outboxRepository, notificationDispatchPort, Optional.empty(), false);
    }

    @Autowired
    public AppointmentOutboxPublisherScheduler(
            AppointmentEventOutboxRepository outboxRepository,
            NotificationDispatchPort notificationDispatchPort,
            Optional<RabbitTemplate> rabbitTemplate,
            @Value("${cozynest.broker.enabled:false}") boolean brokerEnabled) {
        this.outboxRepository = outboxRepository;
        this.notificationDispatchPort = notificationDispatchPort;
        this.rabbitTemplate = rabbitTemplate != null ? rabbitTemplate : Optional.empty();
        this.brokerEnabled = brokerEnabled;
    }

    @Scheduled(fixedDelayString = "${booking.outbox.poll-ms:5000}")
    @Transactional
    public int publishPendingEvents() {
        List<AppointmentEventOutbox> pending = outboxRepository.findUnprocessed();
        int publishedCount = 0;
        for (AppointmentEventOutbox event : pending) {
            boolean deliveredViaBroker = publishToBrokerIfEnabled(event);
            boolean deliveredViaHttp = !deliveredViaBroker && notificationDispatchPort.dispatch(event);
            if (deliveredViaBroker || deliveredViaHttp) {
                event.setProcessed(true);
                event.setProcessedAt(Instant.now());
                outboxRepository.save(event);
                publishedCount++;
            }
        }
        return publishedCount;
    }

    private boolean publishToBrokerIfEnabled(AppointmentEventOutbox event) {
        if (!brokerEnabled || rabbitTemplate.isEmpty()) {
            return false;
        }
        try {
            DomainEventEnvelope envelope = new DomainEventEnvelope(
                    event.getId(),
                    event.getEventType(),
                    1,
                    resolveAggregateType(event.getEventType()),
                    event.getAggregateId(),
                    event.getId(),
                    event.getCreatedAt() != null ? event.getCreatedAt() : Instant.now(),
                    event.getPayload()
            );
            String routingKey = "booking." + event.getEventType().toLowerCase().replace('_', '.');
            rabbitTemplate.get().convertAndSend(EventTopology.EVENTS_EXCHANGE, routingKey, EventEnvelopeCodec.encode(envelope));
            return true;
        } catch (Exception ex) {
            log.warn("Broker publish failed for booking outbox event {}, falling back to HTTP: {}", event.getId(), ex.getMessage());
            return false;
        }
    }

    private String resolveAggregateType(String eventType) {
        if (eventType == null) return "BOOKING";
        if (eventType.startsWith("CONTRACT")) return "CONTRACT";
        if (eventType.startsWith("RENTAL_REQUEST")) return "RENTAL_REQUEST";
        if (eventType.startsWith("SAGA")) return "SAGA";
        return "APPOINTMENT";
    }
}
