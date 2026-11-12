package vn.cozynest.billing.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.domain.model.BillingEventOutbox;
import vn.cozynest.billing.domain.repository.BillingEventOutboxRepository;
import vn.cozynest.common.events.DomainEventEnvelope;
import vn.cozynest.common.events.EventEnvelopeCodec;
import vn.cozynest.common.events.EventTopology;

import java.util.List;
import java.util.Optional;

@Service
public class BillingOutboxPublisherScheduler {
    private static final Logger log = LoggerFactory.getLogger(BillingOutboxPublisherScheduler.class);

    private final BillingEventOutboxRepository outboxRepository;
    private final Optional<RabbitTemplate> rabbitTemplate;
    private final boolean brokerEnabled;

    @Autowired
    public BillingOutboxPublisherScheduler(
            BillingEventOutboxRepository outboxRepository,
            Optional<RabbitTemplate> rabbitTemplate,
            @Value("${cozynest.broker.enabled:false}") boolean brokerEnabled
    ) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.brokerEnabled = brokerEnabled;
    }

    @Scheduled(fixedDelayString = "${cozynest.billing.outbox-poll-ms:10000}")
    @Transactional
    public int publishPendingEvents() {
        if (!brokerEnabled || rabbitTemplate.isEmpty()) {
            return 0;
        }
        List<BillingEventOutbox> pending = outboxRepository.findPendingEvents();
        int sent = 0;
        for (BillingEventOutbox event : pending) {
            try {
                DomainEventEnvelope envelope = new DomainEventEnvelope(
                        event.getId(),
                        event.getEventType(),
                        1,
                        event.getAggregateType(),
                        event.getAggregateId(),
                        event.getId(),
                        event.getCreatedAt(),
                        event.getPayloadJson()
                );
                String routingKey = "billing." + event.getEventType().toLowerCase().replace('_', '.');
                rabbitTemplate.get().convertAndSend(
                        EventTopology.EVENTS_EXCHANGE,
                        routingKey,
                        EventEnvelopeCodec.encode(envelope)
                );
                event.markPublished();
                outboxRepository.save(event);
                sent++;
            } catch (Exception ex) {
                log.warn("Failed publishing billing outbox id={}: {}", event.getId(), ex.getMessage());
                event.markFailedAttempt();
                outboxRepository.save(event);
            }
        }
        return sent;
    }
}

