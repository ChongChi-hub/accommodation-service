package vn.cozynest.booking.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.booking.domain.model.AppointmentEventOutbox;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.booking.infrastructure.client.HttpNotificationDispatchAdapter;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppointmentOutboxPublisherTest {

    @Test
    @DisplayName("Outbox scheduler dispatches pending event over HTTP, marks processed on 200 and preserves unprocessed on 500")
    void shouldDispatchAndMarkProcessedOnSuccessOnly() throws Exception {
        AtomicInteger statusCode = new AtomicInteger(200);
        AtomicInteger requests = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/notifications", exchange -> {
            requests.incrementAndGet();
            byte[] resp = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode.get(), resp.length);
            exchange.getResponseBody().write(resp);
            exchange.close();
        });
        server.start();

        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            HttpNotificationDispatchAdapter adapter = new HttpNotificationDispatchAdapter(
                    baseUrl, "CozyNestInternalServiceKeySecret2026", new ObjectMapper()
            );

            AppointmentEventOutboxRepository repo = mock(AppointmentEventOutboxRepository.class);
            AppointmentEventOutbox event = new AppointmentEventOutbox(
                    "APPOINTMENT_CONFIRMED", "appt-1", "{\"tenantId\":\"u-tenant-01\",\"landlordId\":\"u-landlord-01\"}"
            );
            when(repo.findUnprocessed()).thenReturn(List.of(event));

            AppointmentOutboxPublisherScheduler scheduler = new AppointmentOutboxPublisherScheduler(repo, adapter);
            int published = scheduler.publishPendingEvents();

            assertThat(published).isEqualTo(1);
            assertThat(requests.get()).isEqualTo(1);
            verify(repo).save(argThat(e -> e.isProcessed() && e.getProcessedAt() != null));

            // Simulate transient 500 error -> event must remain unprocessed for retry
            statusCode.set(500);
            AppointmentEventOutbox retryEvent = new AppointmentEventOutbox(
                    "APPOINTMENT_CONFIRMED", "appt-2", "{\"tenantId\":\"u-tenant-01\"}"
            );
            reset(repo);
            when(repo.findUnprocessed()).thenReturn(List.of(retryEvent));

            int failedCount = scheduler.publishPendingEvents();
            assertThat(failedCount).isZero();
            verify(repo, never()).save(any());

            // When RabbitMQ broker is enabled, publishes DomainEventEnvelope to cozynest.events
            var rabbitTemplate = mock(org.springframework.amqp.rabbit.core.RabbitTemplate.class);
            AppointmentOutboxPublisherScheduler brokerScheduler = new AppointmentOutboxPublisherScheduler(
                    repo, adapter, java.util.Optional.of(rabbitTemplate), true
            );
            when(repo.findUnprocessed()).thenReturn(List.of(retryEvent));
            int brokerCount = brokerScheduler.publishPendingEvents();
            assertThat(brokerCount).isEqualTo(1);
            verify(rabbitTemplate).convertAndSend(
                    eq(vn.cozynest.common.events.EventTopology.EVENTS_EXCHANGE),
                    eq("booking.appointment.confirmed"),
                    anyString()
            );
        } finally {
            server.stop(0);
        }
    }
}

