package vn.cozynest.listing.interfaces.rest;

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
public class InternalListingStatsController {

    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final JdbcTemplate jdbcTemplate;
    private final String expectedServiceKey;

    @Autowired
    public InternalListingStatsController(
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

    @GetMapping("/listing")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getListingStats(
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

        long totalRooms = queryLong("SELECT COUNT(*) FROM rooms WHERE deleted_at IS NULL");
        long activeRooms = queryLong(
                "SELECT COUNT(*) FROM rooms WHERE deleted_at IS NULL AND hidden_at IS NULL AND is_approved = 1 AND status IN ('AVAILABLE', 'PUBLISHED')"
        );
        long pendingRooms = queryLong(
                "SELECT COUNT(*) FROM rooms WHERE deleted_at IS NULL AND (is_approved = 0 OR status = 'PENDING_REVIEW')"
        );
        long rejectedRooms = queryLong(
                "SELECT COUNT(*) FROM rooms WHERE deleted_at IS NULL AND status = 'REJECTED'"
        );
        long occupiedRooms = queryLong(
                "SELECT COUNT(*) FROM rooms WHERE deleted_at IS NULL AND is_approved = 1 AND (occupancy_status = 'OCCUPIED' OR status = 'RENTED')"
        );
        long totalViews = queryLong("SELECT COALESCE(SUM(view_count), 0) FROM rooms WHERE deleted_at IS NULL");

        Map<String, Long> roomsByStatus = new LinkedHashMap<>();
        List<Map<String, Object>> statusRows = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) AS cnt FROM rooms WHERE deleted_at IS NULL GROUP BY status ORDER BY status"
        );
        for (Map<String, Object> row : statusRows) {
            roomsByStatus.put(String.valueOf(row.get("status")), ((Number) row.get("cnt")).longValue());
        }

        List<Map<String, Object>> topAreas = new ArrayList<>();
        List<Map<String, Object>> areaRows = jdbcTemplate.queryForList(
                "SELECT district_code, COUNT(*) AS cnt FROM rooms " +
                "WHERE deleted_at IS NULL AND district_code IS NOT NULL AND district_code <> '' " +
                "GROUP BY district_code ORDER BY cnt DESC LIMIT 6"
        );
        long areaTotal = areaRows.stream().mapToLong(r -> ((Number) r.get("cnt")).longValue()).sum();
        int idx = 1;
        for (Map<String, Object> r : areaRows) {
            long cnt = ((Number) r.get("cnt")).longValue();
            int pct = areaTotal > 0 ? (int) Math.round(((double) cnt / areaTotal) * 100.0) : 0;
            String code = String.valueOf(r.get("district_code"));
            Map<String, Object> area = new LinkedHashMap<>();
            area.put("id", String.valueOf(idx++));
            area.put("districtCode", code);
            area.put("key", mapDistrictKey(code));
            area.put("count", cnt);
            area.put("percent", pct);
            topAreas.add(area);
        }

        List<Map<String, Object>> dailyNewRooms = new ArrayList<>();
        List<Map<String, Object>> tsRows = jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS bucket, COUNT(*) AS cnt " +
                "FROM rooms WHERE deleted_at IS NULL AND DATE(created_at) BETWEEN ? AND ? " +
                "GROUP BY bucket ORDER BY bucket",
                fromDate.toString(), toDate.toString()
        );
        for (Map<String, Object> row : tsRows) {
            dailyNewRooms.add(Map.of(
                    "date", String.valueOf(row.get("bucket")),
                    "count", ((Number) row.get("cnt")).longValue()
            ));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("totalRooms", totalRooms);
        payload.put("activeRooms", activeRooms);
        payload.put("pendingRooms", pendingRooms);
        payload.put("rejectedRooms", rejectedRooms);
        payload.put("occupiedRooms", occupiedRooms);
        payload.put("totalViews", totalViews);
        payload.put("roomsByStatus", roomsByStatus);
        payload.put("topAreas", topAreas);
        payload.put("dailyNewRooms", dailyNewRooms);
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    private String mapDistrictKey(String code) {
        String lower = code.toLowerCase();
        if (lower.contains("dong_da") || lower.contains("dongda")) return "areaDongDa";
        if (lower.contains("binh_thanh") || lower.contains("binhthanh")) return "areaBinhThanh";
        if (lower.contains("cau_giay") || lower.contains("caugiay")) return "areaCauGiay";
        return code;
    }

    private long queryLong(String sql, Object... args) {
        Number n = jdbcTemplate.queryForObject(sql, Number.class, args);
        return n != null ? n.longValue() : 0L;
    }

    private boolean isKeyValid(String serviceKey) {
        if (expectedServiceKey == null) return false;
        return MessageDigest.isEqual(
                serviceKey.getBytes(StandardCharsets.UTF_8),
                expectedServiceKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
