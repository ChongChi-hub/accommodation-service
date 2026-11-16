package vn.cozynest.listing.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import vn.cozynest.common.events.EventTopology;
import vn.cozynest.listing.domain.model.ListingEventOutbox;
import vn.cozynest.listing.domain.repository.ListingEventOutboxRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ListingOutboxPublisherSchedulerTest {

    @Test
    @DisplayName("When broker is disabled, listing outbox publisher skips")
    void skipsWhenBrokerDisabled() {
        ListingEventOutboxRepository repo = mock(ListingEventOutboxRepository.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);

        ListingOutboxPublisherScheduler scheduler = new ListingOutboxPublisherScheduler(
                repo, Optional.of(rabbit), false
        );

        int count = scheduler.publishPendingEvents();
        assertThat(count).isZero();
        verifyNoInteractions(repo, rabbit);
    }

    @Test
    @DisplayName("When broker is enabled, publishes listing outbox events to cozynest.events with listing routing key")
    void publishesListingEventsWhenBrokerEnabled() {
        ListingEventOutboxRepository repo = mock(ListingEventOutboxRepository.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);

        ListingEventOutbox event = ListingEventOutbox.create("ROOM", "room-101", "LISTING_PUBLISHED", "{\"title\":\"Phòng trọ Cầu Giấy\"}");
        when(repo.findPending(50)).thenReturn(List.of(event));

        ListingOutboxPublisherScheduler scheduler = new ListingOutboxPublisherScheduler(
                repo, Optional.of(rabbit), true
        );

        int published = scheduler.publishPendingEvents();
        assertThat(published).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo("PROCESSED");

        verify(rabbit).convertAndSend(
                eq(EventTopology.EVENTS_EXCHANGE),
                eq("listing.listing.published"),
                any(String.class)
        );
        verify(repo).save(event);
    }
}
