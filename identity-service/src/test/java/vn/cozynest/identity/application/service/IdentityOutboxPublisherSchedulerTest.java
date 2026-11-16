package vn.cozynest.identity.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import vn.cozynest.common.events.EventTopology;
import vn.cozynest.identity.domain.model.FaceEventOutbox;
import vn.cozynest.identity.domain.model.KycEventOutbox;
import vn.cozynest.identity.domain.repository.FaceEventOutboxRepository;
import vn.cozynest.identity.domain.repository.KycEventOutboxRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.UserEventOutboxJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataUserEventOutboxRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IdentityOutboxPublisherSchedulerTest {

    @Test
    @DisplayName("When broker is disabled, scheduler skips publishing")
    void skipsWhenBrokerDisabled() {
        KycEventOutboxRepository kycRepo = mock(KycEventOutboxRepository.class);
        FaceEventOutboxRepository faceRepo = mock(FaceEventOutboxRepository.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);

        IdentityOutboxPublisherScheduler scheduler = new IdentityOutboxPublisherScheduler(
                kycRepo, faceRepo, Optional.of(rabbit), false
        );

        int count = scheduler.publishPendingEvents();
        assertThat(count).isZero();
        verifyNoInteractions(kycRepo, faceRepo, rabbit);
    }

    @Test
    @DisplayName("When broker is enabled, scheduler publishes KYC, Face, and User outbox events to RabbitMQ")
    void publishesAllOutboxStreamsWhenBrokerEnabled() {
        KycEventOutboxRepository kycRepo = mock(KycEventOutboxRepository.class);
        FaceEventOutboxRepository faceRepo = mock(FaceEventOutboxRepository.class);
        SpringDataUserEventOutboxRepository userRepo = mock(SpringDataUserEventOutboxRepository.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);

        KycEventOutbox kycEvent = KycEventOutbox.create("usr-1", "KYC_APPROVED", "{\"userId\":\"usr-1\"}");
        FaceEventOutbox faceEvent = FaceEventOutbox.create("usr-1", "FACE_VERIFIED", "{\"userId\":\"usr-1\"}");
        UserEventOutboxJpaEntity userEvent = UserEventOutboxJpaEntity.builder()
                .id("u-evt-1")
                .aggregateId("usr-1")
                .eventType("UserRegistered")
                .payload("{\"userId\":\"usr-1\",\"role\":\"TENANT\"}")
                .published(false)
                .createdAt(Instant.now())
                .build();

        when(kycRepo.findPending(50)).thenReturn(List.of(kycEvent));
        when(faceRepo.findUnpublished(50)).thenReturn(List.of(faceEvent));
        when(userRepo.findTop50ByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(userEvent));

        IdentityOutboxPublisherScheduler scheduler = new IdentityOutboxPublisherScheduler(
                kycRepo, faceRepo, Optional.of(userRepo), Optional.of(rabbit), true
        );

        int published = scheduler.publishPendingEvents();
        assertThat(published).isEqualTo(3);

        verify(rabbit).convertAndSend(eq(EventTopology.EVENTS_EXCHANGE), eq("identity.kyc.approved"), any(String.class));
        verify(rabbit).convertAndSend(eq(EventTopology.EVENTS_EXCHANGE), eq("identity.face.verified"), any(String.class));
        verify(rabbit).convertAndSend(eq(EventTopology.EVENTS_EXCHANGE), eq("identity.userregistered"), any(String.class));

        assertThat(kycEvent.getStatus()).isEqualTo("PROCESSED");
        assertThat(faceEvent.isPublished()).isTrue();
        assertThat(userEvent.isPublished()).isTrue();

        verify(kycRepo).save(kycEvent);
        verify(faceRepo).save(faceEvent);
        verify(userRepo).save(userEvent);
    }
}
