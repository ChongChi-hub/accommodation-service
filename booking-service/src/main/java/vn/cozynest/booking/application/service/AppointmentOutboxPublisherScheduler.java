package vn.cozynest.booking.application.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.booking.application.port.NotificationDispatchPort;
import vn.cozynest.booking.domain.model.AppointmentEventOutbox;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;

import java.time.Instant;
import java.util.List;

@Service
public class AppointmentOutboxPublisherScheduler {

    private final AppointmentEventOutboxRepository outboxRepository;
    private final NotificationDispatchPort notificationDispatchPort;

    public AppointmentOutboxPublisherScheduler(
            AppointmentEventOutboxRepository outboxRepository,
            NotificationDispatchPort notificationDispatchPort) {
        this.outboxRepository = outboxRepository;
        this.notificationDispatchPort = notificationDispatchPort;
    }

    @Scheduled(fixedDelayString = "${booking.outbox.poll-ms:5000}")
    @Transactional
    public int publishPendingEvents() {
        List<AppointmentEventOutbox> pending = outboxRepository.findUnprocessed();
        int publishedCount = 0;
        for (AppointmentEventOutbox event : pending) {
            if (notificationDispatchPort.dispatch(event)) {
                event.setProcessed(true);
                event.setProcessedAt(Instant.now());
                outboxRepository.save(event);
                publishedCount++;
            }
        }
        return publishedCount;
    }
}

