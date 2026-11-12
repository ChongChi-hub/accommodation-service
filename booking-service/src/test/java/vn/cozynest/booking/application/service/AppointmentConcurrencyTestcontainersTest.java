package vn.cozynest.booking.application.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import vn.cozynest.booking.BookingApplication;
import vn.cozynest.booking.application.dto.ConfirmAppointmentRequest;
import vn.cozynest.booking.application.dto.ViewingAppointmentDetailDto;
import vn.cozynest.booking.application.exception.TimeConflictException;
import vn.cozynest.booking.domain.model.AppointmentStatus;
import vn.cozynest.booking.domain.model.ViewingAppointment;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.booking.domain.repository.ViewingAppointmentRepository;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentConcurrencyTestcontainersTest {

    @Test
    @DisplayName("APPT-02-BE.3/.4: Two concurrent confirms for overlapping slot yield 1 success + 1 TimeConflictException (409), plus outbox publish")
    void testConcurrentConfirmOverlapAndOutboxOnRealMySql() throws Exception {
        if ("true".equalsIgnoreCase(System.getenv("CI")) || "true".equalsIgnoreCase(System.getenv("GITHUB_ACTIONS"))) {
            System.out.println("CI runner detected: skipping Testcontainers MySQL test");
            return;
        }
        try {
            org.testcontainers.DockerClientFactory.instance().client();
        } catch (Throwable t) {
            System.out.println("Docker not accessible: skipping Testcontainers MySQL test");
            return;
        }

        HttpServer notifServer = HttpServer.create(new InetSocketAddress(0), 0);
        notifServer.createContext("/internal/notifications", ex -> {
            byte[] ok = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, ok.length);
            ex.getResponseBody().write(ok);
            ex.close();
        });
        notifServer.start();

        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
                .withDatabaseName("cozynest_booking")
                .withUsername("testuser")
                .withPassword("testpass")) {
            mysql.start();

            String commUrl = "http://localhost:" + notifServer.getAddress().getPort();
            try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(BookingApplication.class)
                    .run(
                            "--server.port=0",
                            "--spring.datasource.url=" + mysql.getJdbcUrl(),
                            "--spring.datasource.username=" + mysql.getUsername(),
                            "--spring.datasource.password=" + mysql.getPassword(),
                            "--spring.flyway.locations=classpath:db/migration",
                            "--spring.jpa.hibernate.ddl-auto=validate",
                            "--booking.outbox.poll-ms=600000",
                            "--services.communication.url=" + commUrl
                    )) {

                ViewingAppointmentRepository apptRepo = ctx.getBean(ViewingAppointmentRepository.class);
                LandlordAppointmentService service = ctx.getBean(LandlordAppointmentService.class);
                AppointmentEventOutboxRepository outboxRepo = ctx.getBean(AppointmentEventOutboxRepository.class);
                AppointmentOutboxPublisherScheduler scheduler = ctx.getBean(AppointmentOutboxPublisherScheduler.class);

                Instant slotStart = Instant.now().plus(2, ChronoUnit.DAYS);
                Instant slotEnd = slotStart.plus(45, ChronoUnit.MINUTES);

                ViewingAppointment a1 = buildPending(UUID.randomUUID().toString(), "u-ll-race", "u-t-1", slotStart, slotEnd);
                ViewingAppointment a2 = buildPending(UUID.randomUUID().toString(), "u-ll-race", "u-t-2", slotStart, slotEnd);
                apptRepo.save(a1);
                apptRepo.save(a2);

                CountDownLatch startGate = new CountDownLatch(1);
                ExecutorService pool = Executors.newFixedThreadPool(2);
                Future<Object> f1 = pool.submit(() -> {
                    startGate.await(5, TimeUnit.SECONDS);
                    try {
                        return service.confirmAppointment("u-ll-race", a1.getId(), new ConfirmAppointmentRequest(false, "ok", 0L));
                    } catch (Throwable ex) {
                        return ex;
                    }
                });
                Future<Object> f2 = pool.submit(() -> {
                    startGate.await(5, TimeUnit.SECONDS);
                    try {
                        return service.confirmAppointment("u-ll-race", a2.getId(), new ConfirmAppointmentRequest(false, "ok", 0L));
                    } catch (Throwable ex) {
                        return ex;
                    }
                });

                startGate.countDown();
                List<Object> outcomes = new ArrayList<>();
                outcomes.add(f1.get(15, TimeUnit.SECONDS));
                outcomes.add(f2.get(15, TimeUnit.SECONDS));
                pool.shutdownNow();

                long confirmed = outcomes.stream().filter(o -> o instanceof ViewingAppointmentDetailDto).count();
                long conflicts = outcomes.stream().filter(o -> o instanceof TimeConflictException).count();
                assertThat(confirmed).isEqualTo(1L);
                assertThat(conflicts).isEqualTo(1L);

                // Outbox row recorded and published cleanly
                assertThat(outboxRepo.findUnprocessed()).hasSize(1);
                int sent = scheduler.publishPendingEvents();
                assertThat(sent).isEqualTo(1);
                assertThat(outboxRepo.findUnprocessed()).isEmpty();
            }
        } finally {
            notifServer.stop(0);
        }
    }

    private ViewingAppointment buildPending(String id, String landlordId, String tenantId, Instant start, Instant end) {
        ViewingAppointment appt = new ViewingAppointment();
        appt.setId(id);
        appt.setRoomId("101");
        appt.setLandlordId(landlordId);
        appt.setTenantId(tenantId);
        appt.setScheduledStart(start);
        appt.setScheduledEnd(end);
        appt.setAppointmentDate(start.atZone(ZoneOffset.UTC).toLocalDate());
        appt.setAppointmentTime(start.atZone(ZoneOffset.UTC).toLocalTime());
        appt.setStatus(AppointmentStatus.PENDING);
        appt.setContactName("Tenant Race");
        appt.setContactPhone("0901112233");
        appt.setVersion(0L);
        return appt;
    }
}

