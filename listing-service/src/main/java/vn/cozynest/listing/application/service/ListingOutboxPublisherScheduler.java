package vn.cozynest.listing.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.common.events.DomainEventEnvelope;
import vn.cozynest.common.events.EventEnvelopeCodec;
import vn.cozynest.common.events.EventTopology;
import vn.cozynest.listing.domain.model.ListingEventOutbox;
import vn.cozynest.listing.domain.repository.ListingEventOutboxRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ListingOutboxPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(ListingOutboxPublisherScheduler.class);

    private final ListingEventOutboxRepository outboxRepository;
    private final Optional<RabbitTemplate> rabbitTemplate;
    private final boolean brokerEnabled;

    public ListingOutboxPublisherScheduler(ListingEventOutboxRepository outboxRepository) {
        this(outboxRepository, Optional.empty(), false);
    }

    @Autowired
    public ListingOutboxPublisherScheduler(
            ListingEventOutboxRepository outboxRepository,
            Optional<RabbitTemplate> rabbitTemplate,
            @Value("${cozynest.broker.enabled:false}") boolean brokerEnabled
    ) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate != null ? rabbitTemplate : Optional.empty();
        this.brokerEnabled = brokerEnabled;
    }

    @Scheduled(fixedDelayString = "${listing.outbox.poll-ms:5000}")
    @Transactional
    public int publishPendingEvents() {
        if (!brokerEnabled || rabbitTemplate.isEmpty()) {
            return 0;
        }

        List<ListingEventOutbox> pending = outboxRepository.findPending(50);
        int count = 0;
        for (ListingEventOutbox event : pending) {
            try {
                DomainEventEnvelope envelope = new DomainEventEnvelope(
                        event.getId(),
                        event.getEventType(),
                        1,
                        event.getAggregateType(),
                        event.getAggregateId(),
                        event.getId(),
                        event.getCreatedAt(),
                        event.getPayload()
                );
                String routingKey = "listing." + event.getEventType().toLowerCase().replace('_', '.');
                rabbitTemplate.get().convertAndSend(
                        EventTopology.EVENTS_EXCHANGE,
                        routingKey,
                        EventEnvelopeCodec.encode(envelope)
                );
                event.markProcessed();
                outboxRepository.save(event);
                count++;
            } catch (Exception ex) {
                log.warn("Failed publishing listing outbox id={}: {}", event.getId(), ex.getMessage());
                event.markFailedAttempt();
                outboxRepository.save(event);
            }
        }
        return count;
    }
}
