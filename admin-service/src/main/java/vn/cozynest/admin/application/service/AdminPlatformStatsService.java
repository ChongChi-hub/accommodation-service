package vn.cozynest.admin.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.cozynest.admin.application.port.PlatformStatsSourcePort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class AdminPlatformStatsService {

    private static final Logger log = LoggerFactory.getLogger(AdminPlatformStatsService.class);
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    private static final Set<String> VALID_METRICS = Set.of("users", "rooms", "appointments", "contracts", "revenue");
    private static final Set<String> VALID_GRANULARITIES = Set.of("day", "week", "month");

    private final PlatformStatsSourcePort sourcePort;
    private final Clock clock;
    private final ConcurrentHashMap<String, CachedEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public AdminPlatformStatsService(PlatformStatsSourcePort sourcePort) {
        this(sourcePort, Clock.systemUTC());
    }

    public AdminPlatformStatsService(PlatformStatsSourcePort sourcePort, Clock clock) {
        this.sourcePort = sourcePort;
        this.clock = clock;
    }

    public void clearCache() {
        cache.clear();
    }

    public Map<String, Object> getOverview(LocalDate from, LocalDate to) {
        DateRange range = validateRange(from, to);
        String key = "overview:" + range.from + ":" + range.to;
        CachedEntry existing = cache.get(key);
        Instant now = clock.instant();
        if (existing != null && existing.expiresAt.isAfter(now)) {
            return existing.payload;
        }

        Map<String, String> sourceStatus = new LinkedHashMap<>();
        List<String> failedSources = new ArrayList<>();

        Map<String, Object> identity = fetchSafe("identity", () -> sourcePort.fetchIdentityStats(range.from, range.to), sourceStatus, failedSources);
        Map<String, Object> listing = fetchSafe("listing", () -> sourcePort.fetchListingStats(range.from, range.to), sourceStatus, failedSources);
        Map<String, Object> booking = fetchSafe("booking", () -> sourcePort.fetchBookingStats(range.from, range.to), sourceStatus, failedSources);
        Map<String, Object> billing = fetchSafe("billing", () -> sourcePort.fetchBillingStats(range.from, range.to), sourceStatus, failedSources);

        boolean degraded = !failedSources.isEmpty();

        Double successRate = computeSuccessRate(booking, listing);
        Double avgTimeToFillDays = booking != null ? toDouble(booking.get("avgTimeToFillDays")) : null;

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("from", range.from.toString());
        overview.put("to", range.to.toString());
        overview.put("generatedAt", now.toString());
        overview.put("degraded", degraded);
        overview.put("failedSources", failedSources);
        overview.put("sourceStatus", sourceStatus);
        overview.put("identity", identity);
        overview.put("listing", listing);
        overview.put("booking", booking);
        overview.put("billing", billing);
        overview.put("kpis", buildKpis(identity, listing, booking, billing, successRate, avgTimeToFillDays));

        if (!degraded) {
            cache.put(key, new CachedEntry(overview, now.plus(CACHE_TTL)));
        }
        return overview;
    }

    public Map<String, Object> getTimeseries(String metric, LocalDate from, LocalDate to, String granularity) {
        String normMetric = (metric == null || metric.isBlank()) ? "appointments" : metric.trim().toLowerCase();
        if (!VALID_METRICS.contains(normMetric)) {
            throw new IllegalArgumentException("Invalid metric: " + metric + ". Allowed: " + VALID_METRICS);
        }
        String normGranularity = (granularity == null || granularity.isBlank()) ? "day" : granularity.trim().toLowerCase();
        if (!VALID_GRANULARITIES.contains(normGranularity)) {
            throw new IllegalArgumentException("Invalid granularity: " + granularity + ". Allowed: " + VALID_GRANULARITIES);
        }

        DateRange range = validateRange(from, to);
        Map<String, Object> overview = getOverview(range.from, range.to);
        String sourceKey = switch (normMetric) {
            case "users" -> "identity";
            case "rooms" -> "listing";
            case "appointments", "contracts" -> "booking";
            case "revenue" -> "billing";
            default -> "booking";
        };

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceData = (Map<String, Object>) overview.get(sourceKey);
        boolean sourceUnavailable = sourceData == null;
        List<Map<String, Object>> points = sourceUnavailable
                ? List.of()
                : aggregatePoints(extractDailySeries(sourceData, normMetric), normGranularity, "revenue".equals(normMetric));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metric", normMetric);
        result.put("granularity", normGranularity);
        result.put("from", range.from.toString());
        result.put("to", range.to.toString());
        result.put("degraded", sourceUnavailable);
        result.put("source", sourceKey);
        result.put("points", points);
        return result;
    }

    public Map<String, Object> getLegacyStats() {
        LocalDate today = LocalDate.now(clock);
        Map<String, Object> overview = getOverview(today.minusDays(29), today);
        @SuppressWarnings("unchecked")
        Map<String, Object> kpis = (Map<String, Object>) overview.get("kpis");
        @SuppressWarnings("unchecked")
        Map<String, String> sourceStatus = (Map<String, String>) overview.get("sourceStatus");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", kpis.get("totalUsers"));
        result.put("activeRooms", kpis.get("activeRooms"));
        result.put("pendingApprovals", kpis.get("pendingRooms"));
        result.put("pendingKyc", kpis.get("pendingKyc"));
        result.put("monthlyRevenue", kpis.get("paidRevenue"));
        result.put("degraded", overview.get("degraded"));
        result.put("fintechMode", kpis.get("fintechMode"));

        Map<String, Boolean> health = new LinkedHashMap<>();
        health.put("gateway", true);
        health.put("identity", "OK".equals(sourceStatus.get("identity")));
        health.put("listing", "OK".equals(sourceStatus.get("listing")));
        health.put("booking", "OK".equals(sourceStatus.get("booking")));
        health.put("billing", "OK".equals(sourceStatus.get("billing")));
        result.put("servicesHealth", health);
        return result;
    }

    public Map<String, Object> getLegacyAnalytics() {
        LocalDate today = LocalDate.now(clock);
        Map<String, Object> overview = getOverview(today.minusDays(29), today);
        @SuppressWarnings("unchecked")
        Map<String, Object> kpis = (Map<String, Object>) overview.get("kpis");
        @SuppressWarnings("unchecked")
        Map<String, Object> listing = (Map<String, Object>) overview.get("listing");

        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("successRate", kpis.get("successRate"));
        analytics.put("avgTimeToFillDays", kpis.get("avgTimeToFillDays"));
        analytics.put("totalSearches", kpis.get("totalViews"));
        analytics.put("topAreas", listing != null ? listing.getOrDefault("topAreas", List.of()) : List.of());
        analytics.put("degraded", overview.get("degraded"));
        analytics.put("failedSources", overview.get("failedSources"));
        return analytics;
    }

    private Map<String, Object> buildKpis(
            Map<String, Object> identity,
            Map<String, Object> listing,
            Map<String, Object> booking,
            Map<String, Object> billing,
            Double successRate,
            Double avgTimeToFillDays
    ) {
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalUsers", identity != null ? identity.get("totalUsers") : null);
        kpis.put("newUsersInRange", identity != null ? identity.get("newUsersInRange") : null);
        kpis.put("pendingKyc", identity != null ? identity.get("pendingKyc") : null);
        kpis.put("totalRooms", listing != null ? listing.get("totalRooms") : null);
        kpis.put("activeRooms", listing != null ? listing.get("activeRooms") : null);
        kpis.put("pendingRooms", listing != null ? listing.get("pendingRooms") : null);
        kpis.put("rejectedRooms", listing != null ? listing.get("rejectedRooms") : null);
        kpis.put("totalViews", listing != null ? listing.get("totalViews") : null);
        kpis.put("totalAppointments", booking != null ? booking.get("totalAppointments") : null);
        kpis.put("completedAppointments", booking != null ? booking.get("completedAppointments") : null);
        kpis.put("totalRentalRequests", booking != null ? booking.get("totalRentalRequests") : null);
        kpis.put("totalContracts", booking != null ? booking.get("totalContracts") : null);
        kpis.put("activeContracts", booking != null ? booking.get("activeContracts") : null);
        kpis.put("totalInvoices", billing != null ? billing.get("totalInvoices") : null);
        kpis.put("paidInvoices", billing != null ? billing.get("paidInvoices") : null);
        kpis.put("paidRevenue", billing != null ? billing.get("paidRevenue") : null);
        kpis.put("fintechMode", billing != null ? billing.get("fintechMode") : null);
        kpis.put("successRate", successRate);
        kpis.put("avgTimeToFillDays", avgTimeToFillDays);
        return kpis;
    }

    private Double computeSuccessRate(Map<String, Object> booking, Map<String, Object> listing) {
        if (booking != null) {
            long completed = toLong(booking.get("completedAppointments"));
            long activeContracts = toLong(booking.get("activeContracts"));
            if (completed > 0) {
                return Math.round(((double) activeContracts / completed) * 1000.0) / 10.0;
            }
        }
        if (listing != null) {
            long totalRooms = toLong(listing.get("totalRooms"));
            long occupied = toLong(listing.get("occupiedRooms"));
            if (totalRooms > 0) {
                return Math.round(((double) occupied / totalRooms) * 1000.0) / 10.0;
            }
        }
        return (booking == null && listing == null) ? null : 0.0;
    }

    private Map<String, Object> fetchSafe(
            String name,
            Supplier<Map<String, Object>> fetcher,
            Map<String, String> sourceStatus,
            List<String> failedSources
    ) {
        try {
            Map<String, Object> data = fetcher.get();
            sourceStatus.put(name, "OK");
            return data;
        } catch (RuntimeException ex) {
            log.warn("Admin platform stats source '{}' unavailable: {}", name, ex.getMessage());
            sourceStatus.put(name, "UNAVAILABLE");
            failedSources.add(name);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractDailySeries(Map<String, Object> sourceData, String metric) {
        String key = switch (metric) {
            case "users" -> "dailyRegistrations";
            case "rooms" -> "dailyNewRooms";
            case "appointments" -> "dailyAppointments";
            case "contracts" -> "dailyContracts";
            case "revenue" -> "dailyRevenue";
            default -> "dailyAppointments";
        };
        Object raw = sourceData.get(key);
        return (raw instanceof List<?> list) ? (List<Map<String, Object>>) list : List.of();
    }

    private List<Map<String, Object>> aggregatePoints(List<Map<String, Object>> raw, String granularity, boolean useAmount) {
        Map<String, Long> buckets = new TreeMap<>();
        for (Map<String, Object> row : raw) {
            String dateStr = String.valueOf(row.get("date"));
            LocalDate d = LocalDate.parse(dateStr);
            String bucketKey = switch (granularity) {
                case "week" -> d.getYear() + "-W" + String.format("%02d", d.get(WeekFields.ISO.weekOfWeekBasedYear()));
                case "month" -> String.format("%04d-%02d", d.getYear(), d.getMonthValue());
                default -> dateStr;
            };
            long value = toLong(useAmount ? row.get("amount") : row.get("count"));
            buckets.merge(bucketKey, value, Long::sum);
        }
        List<Map<String, Object>> points = new ArrayList<>();
        for (Map.Entry<String, Long> entry : buckets.entrySet()) {
            points.add(Map.of("bucket", entry.getKey(), "value", entry.getValue()));
        }
        return points;
    }

    private DateRange validateRange(LocalDate from, LocalDate to) {
        LocalDate effectiveTo = (to != null) ? to : LocalDate.now(clock);
        LocalDate effectiveFrom = (from != null) ? from : effectiveTo.minusDays(29);
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("Invalid date range: 'from' must be on or before 'to'");
        }
        if (ChronoUnit.DAYS.between(effectiveFrom, effectiveTo) > 366) {
            throw new IllegalArgumentException("Invalid date range: maximum range is 366 days");
        }
        return new DateRange(effectiveFrom, effectiveTo);
    }

    private long toLong(Object val) {
        return (val instanceof Number n) ? n.longValue() : 0L;
    }

    private double toDouble(Object val) {
        return (val instanceof Number n) ? n.doubleValue() : 0.0;
    }

    private record DateRange(LocalDate from, LocalDate to) {}
    private record CachedEntry(Map<String, Object> payload, Instant expiresAt) {}
}
