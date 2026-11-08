package vn.cozynest.booking.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.booking.domain.model.AppointmentEventOutbox;
import vn.cozynest.booking.domain.model.AppointmentStatusHistory;
import vn.cozynest.booking.domain.model.ViewingAppointment;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.booking.domain.repository.AppointmentStatusHistoryRepository;
import vn.cozynest.booking.domain.repository.ViewingAppointmentRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class AppointmentExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentExpirationScheduler.class);

    private final ViewingAppointmentRepository appointmentRepository;
    private final AppointmentStatusHistoryRepository historyRepository;
    private final AppointmentEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public AppointmentExpirationScheduler(
            ViewingAppointmentRepository appointmentRepository,
            AppointmentStatusHistoryRepository historyRepository,
            AppointmentEventOutboxRepository outboxRepository,
            ObjectMapper objectMapper
    ) {
        this.appointmentRepository = appointmentRepository;
        this.historyRepository = historyRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expirePendingPastAppointments() {
        Instant now = Instant.now();
        List<ViewingAppointment> expired = appointmentRepository.findExpiredPending(now);
        if (expired.isEmpty()) {
            return;
        }

        log.info("Found {} past pending appointments to expire", expired.size());
        for (ViewingAppointment appt : expired) {
            try {
                appt.expire();
                appointmentRepository.save(appt);

                historyRepository.save(new AppointmentStatusHistory(
                        appt.getId(), "PENDING", "EXPIRED", "SYSTEM", "SYSTEM", "Qua gio hen khong duoc xu ly"
                ));

                String payload = objectMapper.writeValueAsString(Map.of(
                        "appointmentId", appt.getId(),
                        "landlordId", appt.getLandlordId(),
                        "tenantId", appt.getTenantId(),
                        "scheduledStart", appt.getScheduledStart().toString()
                ));
                outboxRepository.save(new AppointmentEventOutbox("APPOINTMENT_EXPIRED", appt.getId(), payload));
            } catch (Exception e) {
                log.error("Failed to expire appointment id={}: {}", appt.getId(), e.getMessage());
            }
        }
    }
}
