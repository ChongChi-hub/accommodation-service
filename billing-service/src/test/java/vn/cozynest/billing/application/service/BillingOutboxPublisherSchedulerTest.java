package vn.cozynest.billing.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import vn.cozynest.billing.domain.model.BillingEventOutbox;
import vn.cozynest.billing.domain.repository.BillingEventOutboxRepository;
import vn.cozynest.common.events.EventTopology;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BillingOutboxPublisherSchedulerTest {

    @Test
    @DisplayName("When broker is disabled, billing outbox publisher skips")
    void skipsWhenBrokerDisabled() {
        BillingEventOutboxRepository repo = mock(BillingEventOutboxRepository.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);

        BillingOutboxPublisherScheduler scheduler = new BillingOutboxPublisherScheduler(
                repo, Optional.of(rabbit), false
        );

        int sent = scheduler.publishPendingEvents();
        assertThat(sent).isZero();
        verifyNoInteractions(repo, rabbit);
    }

    @Test
    @DisplayName("When broker is enabled, publishes billing outbox events to cozynest.events with billing routing key")
    void publishesBillingEventsWhenBrokerEnabled() {
        BillingEventOutboxRepository repo = mock(BillingEventOutboxRepository.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);

        BillingEventOutbox event = BillingEventOutbox.create(
                "INVOICE_GENERATED", "INVOICE", "inv-101",
                "{\"invoiceId\":\"inv-101\",\"tenantId\":\"u-101\",\"totalAmount\":3500000}"
        );
        when(repo.findPendingEvents()).thenReturn(List.of(event));

        BillingOutboxPublisherScheduler scheduler = new BillingOutboxPublisherScheduler(
                repo, Optional.of(rabbit), true
        );

        int sent = scheduler.publishPendingEvents();
        assertThat(sent).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo("PUBLISHED");

        verify(rabbit).convertAndSend(
                eq(EventTopology.EVENTS_EXCHANGE),
                eq("billing.invoice.generated"),
                any(String.class)
        );
        verify(repo).save(event);
    }
}
