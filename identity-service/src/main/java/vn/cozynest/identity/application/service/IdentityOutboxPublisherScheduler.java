package vn.cozynest.identity.application.service;

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
import vn.cozynest.identity.domain.model.FaceEventOutbox;
import vn.cozynest.identity.domain.model.KycEventOutbox;
import vn.cozynest.identity.domain.repository.FaceEventOutboxRepository;
import vn.cozynest.identity.domain.repository.KycEventOutboxRepository;

import java.util.List;
import java.util.Optional;

@Service
public class IdentityOutboxPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(IdentityOutboxPublisherScheduler.class);

    private final KycEventOutboxRepository kycOutboxRepository;
    private final FaceEventOutboxRepository faceOutboxRepository;
    private final Optional<RabbitTemplate> rabbitTemplate;
    private final boolean brokerEnabled;

    public IdentityOutboxPublisherScheduler(
            KycEventOutboxRepository kycOutboxRepository,
            FaceEventOutboxRepository faceOutboxRepository) {
        this(kycOutboxRepository, faceOutboxRepository, Optional.empty(), false);
    }

    @Autowired
    public IdentityOutboxPublisherScheduler(
            KycEventOutboxRepository kycOutboxRepository,
            FaceEventOutboxRepository faceOutboxRepository,
            Optional<RabbitTemplate> rabbitTemplate,
            @Value("${cozynest.broker.enabled:false}") boolean brokerEnabled) {
        this.kycOutboxRepository = kycOutboxRepository;
        this.faceOutboxRepository = faceOutboxRepository;
        this.rabbitTemplate = rabbitTemplate != null ? rabbitTemplate : Optional.empty();
        this.brokerEnabled = brokerEnabled;
    }

    @Scheduled(fixedDelayString = "${identity.outbox.poll-ms:5000}")
    @Transactional
    public int publishPendingEvents() {
        if (!brokerEnabled || rabbitTemplate.isEmpty()) {
            return 0;
        }

        int publishedCount = 0;
        publishedCount += publishKycEvents();
        publishedCount += publishFaceEvents();
        return publishedCount;
    }

    private int publishKycEvents() {
        List<KycEventOutbox> pendingKyc = kycOutboxRepository.findPending(50);
        int count = 0;
        for (KycEventOutbox event : pendingKyc) {
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
                String routingKey = "identity." + event.getEventType().toLowerCase().replace('_', '.');
                rabbitTemplate.get().convertAndSend(
                        EventTopology.EVENTS_EXCHANGE,
                        routingKey,
                        EventEnvelopeCodec.encode(envelope)
                );
                event.markProcessed();
                kycOutboxRepository.save(event);
                count++;
            } catch (Exception ex) {
                log.warn("Failed publishing identity kyc outbox id={}: {}", event.getId(), ex.getMessage());
            }
        }
        return count;
    }

    private int publishFaceEvents() {
        List<FaceEventOutbox> pendingFace = faceOutboxRepository.findUnpublished(50);
        int count = 0;
        for (FaceEventOutbox event : pendingFace) {
            try {
                DomainEventEnvelope envelope = new DomainEventEnvelope(
                        event.getId(),
                        event.getEventType(),
                        1,
                        "FACE_VERIFICATION",
                        event.getAggregateId(),
                        event.getId(),
                        event.getCreatedAt(),
                        event.getPayload()
                );
                String routingKey = "identity." + event.getEventType().toLowerCase().replace('_', '.');
                rabbitTemplate.get().convertAndSend(
                        EventTopology.EVENTS_EXCHANGE,
                        routingKey,
                        EventEnvelopeCodec.encode(envelope)
                );
                event.markPublished();
                faceOutboxRepository.save(event);
                count++;
            } catch (Exception ex) {
                log.warn("Failed publishing identity face outbox id={}: {}", event.getId(), ex.getMessage());
            }
        }
        return count;
    }
}
