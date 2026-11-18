package vn.cozynest.booking.interfaces.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.cozynest.common.dto.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/stats")
public class InternalBookingStatsController {

    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final JdbcTemplate jdbcTemplate;
    private final String expectedServiceKey;

    @Autowired
    public InternalBookingStatsController(
            JdbcTemplate jdbcTemplate,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String expectedServiceKey,
            @Autowired(required = false) Environment env
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.expectedServiceKey = expectedServiceKey;
        if (env != null && env.matchesProfiles("prod", "production", "staging")) {
            if (DEFAULT_INTERNAL_KEY.equals(expectedServiceKey)) {
                throw new IllegalStateException("Restricted environment requires explicit app.internal.service-key");
            }
        }
    }

    @GetMapping("/booking")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBookingStats(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey
    ) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid internal service key", "ERR_UNAUTHORIZED"));
        }

        LocalDate toDate = (to != null && !to.isBlank()) ? LocalDate.parse(to) : LocalDate.now();
        LocalDate fromDate = (from != null && !from.isBlank()) ? LocalDate.parse(from) : toDate.minusDays(29);

        Map<String, Long> appointmentsByStatus = queryStatusCounts("viewing_appointments");
        Map<String, Long> rentalRequestsByStatus = queryStatusCounts("rental_requests");
        Map<String, Long> contractsByStatus = queryStatusCounts("contracts");

        long totalAppointments = appointmentsByStatus.values().stream().mapToLong(Long::longValue).sum();
        long completedAppointments = appointmentsByStatus.getOrDefault("COMPLETED", 0L);
        long totalRentalRequests = rentalRequestsByStatus.values().stream().mapToLong(Long::longValue).sum();
        long totalContracts = contractsByStatus.values().stream().mapToLong(Long::longValue).sum();
        long activeContracts = contractsByStatus.getOrDefault("ACTIVE", 0L)
                + contractsByStatus.getOrDefault("COMPLETED", 0L);

        Double rawAvgDays = jdbcTemplate.queryForObject(
                "SELECT AVG(GREATEST(DATEDIFF(c.created_at, va.created_at), 0)) " +
                "FROM contracts c " +
                "JOIN viewing_appointments va ON c.room_id = va.room_id AND c.tenant_id = va.tenant_id",
                Double.class
        );
        double avgTimeToFillDays = (rawAvgDays != null && rawAvgDays > 0)
                ? Math.round(rawAvgDays * 10.0) / 10.0
                : 0.0;

        List<Map<String, Object>> dailyAppointments = queryDailyCounts("viewing_appointments", fromDate, toDate);
        List<Map<String, Object>> dailyContracts = queryDailyCounts("contracts", fromDate, toDate);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("totalAppointments", totalAppointments);
        payload.put("completedAppointments", completedAppointments);
        payload.put("appointmentsByStatus", appointmentsByStatus);
        payload.put("totalRentalRequests", totalRentalRequests);
        payload.put("rentalRequestsByStatus", rentalRequestsByStatus);
        payload.put("totalContracts", totalContracts);
        payload.put("activeContracts", activeContracts);
        payload.put("contractsByStatus", contractsByStatus);
        payload.put("avgTimeToFillDays", avgTimeToFillDays);
        payload.put("dailyAppointments", dailyAppointments);
        payload.put("dailyContracts", dailyContracts);
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    private Map<String, Long> queryStatusCounts(String table) {
        Map<String, Long> map = new LinkedHashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) AS cnt FROM " + table + " GROUP BY status ORDER BY status"
        );
        for (Map<String, Object> row : rows) {
            map.put(String.valueOf(row.get("status")), ((Number) row.get("cnt")).longValue());
        }
        return map;
    }

    private List<Map<String, Object>> queryDailyCounts(String table, LocalDate fromDate, LocalDate toDate) {
        List<Map<String, Object>> list = new ArrayList<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS bucket, COUNT(*) AS cnt " +
                "FROM " + table + " WHERE DATE(created_at) BETWEEN ? AND ? " +
                "GROUP BY bucket ORDER BY bucket",
                fromDate.toString(), toDate.toString()
        );
        for (Map<String, Object> row : rows) {
            list.add(Map.of(
                    "date", String.valueOf(row.get("bucket")),
                    "count", ((Number) row.get("cnt")).longValue()
            ));
        }
        return list;
    }

    private boolean isKeyValid(String serviceKey) {
        if (expectedServiceKey == null) return false;
        return MessageDigest.isEqual(
                serviceKey.getBytes(StandardCharsets.UTF_8),
                expectedServiceKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
