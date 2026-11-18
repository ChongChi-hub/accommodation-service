package vn.cozynest.admin.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import vn.cozynest.admin.application.port.PlatformStatsSourcePort;
import vn.cozynest.admin.interfaces.rest.AdminPlatformStatsController;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminPlatformStatsServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-11-19T00:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("Aggregates all 4 real sources, computes KPIs, caches 30s, and invalidates on clearCache")
    void aggregatesAllSourcesAndCaches() {
        AtomicInteger calls = new AtomicInteger(0);
        PlatformStatsSourcePort port = new StubPort(calls, false);
        AdminPlatformStatsService service = new AdminPlatformStatsService(port, FIXED_CLOCK);

        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 19);

        Map<String, Object> first = service.getOverview(from, to);
        assertThat(first.get("degraded")).isEqualTo(false);
        assertThat((List<?>) first.get("failedSources")).isEmpty();

        @SuppressWarnings("unchecked")
        Map<String, Object> kpis = (Map<String, Object>) first.get("kpis");
        assertThat(kpis.get("totalUsers")).isEqualTo(25L);
        assertThat(kpis.get("activeRooms")).isEqualTo(69L);
        assertThat(kpis.get("completedAppointments")).isEqualTo(8L);
        assertThat(kpis.get("activeContracts")).isEqualTo(2L);
        assertThat(kpis.get("successRate")).isEqualTo(25.0);
        assertThat(kpis.get("paidRevenue")).isEqualTo(9695000L);
        assertThat(kpis.get("fintechMode")).isEqualTo("SANDBOX_ONLY");

        // Second call hits cache
        service.getOverview(from, to);
        assertThat(calls.get()).isEqualTo(4);

        // Clear cache forces fresh fetch
        service.clearCache();
        service.getOverview(from, to);
        assertThat(calls.get()).isEqualTo(8);
    }

    @Test
    @DisplayName("Marks degraded=true and returns null (never fake 0) when a downstream source fails")
    void marksDegradedWhenBillingFails() {
        AtomicInteger calls = new AtomicInteger(0);
        PlatformStatsSourcePort port = new StubPort(calls, true);
        AdminPlatformStatsService service = new AdminPlatformStatsService(port, FIXED_CLOCK);

        Map<String, Object> overview = service.getOverview(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 19));
        assertThat(overview.get("degraded")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        List<String> failedSources = (List<String>) overview.get("failedSources");
        assertThat(failedSources).containsExactly("billing");

        @SuppressWarnings("unchecked")
        Map<String, Object> kpis = (Map<String, Object>) overview.get("kpis");
        assertThat(kpis.get("totalUsers")).isEqualTo(25L);
        assertThat(kpis.get("paidRevenue")).isNull();
        assertThat(kpis.get("totalInvoices")).isNull();
    }

    @Test
    @DisplayName("Rejects invalid date ranges, metrics, and granularities with IllegalArgumentException (400)")
    void rejectsInvalidParameters() {
        AdminPlatformStatsService service = new AdminPlatformStatsService(new StubPort(new AtomicInteger(), false), FIXED_CLOCK);

        assertThatThrownBy(() -> service.getOverview(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from");

        assertThatThrownBy(() -> service.getTimeseries("invalid_metric", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 19), "day"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metric");

        assertThatThrownBy(() -> service.getTimeseries("users", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 19), "year"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("granularity");
    }

    @Test
    @DisplayName("Aggregates timeseries by day, week, and month")
    void aggregatesTimeseries() {
        AdminPlatformStatsService service = new AdminPlatformStatsService(new StubPort(new AtomicInteger(), false), FIXED_CLOCK);
        Map<String, Object> monthly = service.getTimeseries("revenue", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 19), "month");
        assertThat(monthly.get("degraded")).isEqualTo(false);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> points = (List<Map<String, Object>>) monthly.get("points");
        assertThat(points).hasSize(1);
        assertThat(points.get(0).get("bucket")).isEqualTo("2026-11");
        assertThat(points.get(0).get("value")).isEqualTo(9695000L);
    }

    @Test
    @DisplayName("AdminPlatformStatsController strictly enforces ADMIN-only role via @PreAuthorize")
    void enforcesAdminOnlySecurity() throws NoSuchMethodException {
        Method overview = AdminPlatformStatsController.class.getMethod("getOverview", LocalDate.class, LocalDate.class);
        Method timeseries = AdminPlatformStatsController.class.getMethod("getTimeseries", String.class, LocalDate.class, LocalDate.class, String.class);

        assertThat(overview.getAnnotation(PreAuthorize.class)).isNotNull();
        assertThat(overview.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
        assertThat(timeseries.getAnnotation(PreAuthorize.class)).isNotNull();
        assertThat(timeseries.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
    }

    private static final class StubPort implements PlatformStatsSourcePort {
        private final AtomicInteger calls;
        private final boolean failBilling;

        private StubPort(AtomicInteger calls, boolean failBilling) {
            this.calls = calls;
            this.failBilling = failBilling;
        }

        @Override
        public Map<String, Object> fetchIdentityStats(LocalDate from, LocalDate to) {
            calls.incrementAndGet();
            return Map.of(
                    "totalUsers", 25L,
                    "newUsersInRange", 5L,
                    "pendingKyc", 2L,
                    "usersByRole", Map.of("TENANT", 20L, "LANDLORD", 6L, "ADMIN", 2L),
                    "dailyRegistrations", List.of(Map.of("date", "2026-11-10", "count", 3L))
            );
        }

        @Override
        public Map<String, Object> fetchListingStats(LocalDate from, LocalDate to) {
            calls.incrementAndGet();
            return Map.of(
                    "totalRooms", 85L,
                    "activeRooms", 69L,
                    "pendingRooms", 3L,
                    "rejectedRooms", 2L,
                    "occupiedRooms", 5L,
                    "totalViews", 420L,
                    "topAreas", List.of(Map.of("id", "1", "districtCode", "cau_giay", "key", "areaCauGiay", "count", 30L, "percent", 50)),
                    "dailyNewRooms", List.of(Map.of("date", "2026-11-10", "count", 2L))
            );
        }

        @Override
        public Map<String, Object> fetchBookingStats(LocalDate from, LocalDate to) {
            calls.incrementAndGet();
            return Map.of(
                    "totalAppointments", 47L,
                    "completedAppointments", 8L,
                    "totalRentalRequests", 8L,
                    "totalContracts", 6L,
                    "activeContracts", 2L,
                    "avgTimeToFillDays", 3.5,
                    "dailyAppointments", List.of(Map.of("date", "2026-11-10", "count", 4L)),
                    "dailyContracts", List.of(Map.of("date", "2026-11-12", "count", 1L))
            );
        }

        @Override
        public Map<String, Object> fetchBillingStats(LocalDate from, LocalDate to) {
            calls.incrementAndGet();
            if (failBilling) {
                throw new IllegalStateException("billing timeout");
            }
            return Map.of(
                    "totalInvoices", 4L,
                    "paidInvoices", 2L,
                    "paidRevenue", 9695000L,
                    "fintechMode", "SANDBOX_ONLY",
                    "dailyRevenue", List.of(
                            Map.of("date", "2026-11-05", "amount", 5000000L, "count", 1L),
                            Map.of("date", "2026-11-15", "amount", 4695000L, "count", 1L)
                    )
            );
        }
    }
}
