package vn.cozynest.identity.interfaces.rest;

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
public class InternalIdentityStatsController {

    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final JdbcTemplate jdbcTemplate;
    private final String expectedServiceKey;

    @Autowired
    public InternalIdentityStatsController(
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

    @GetMapping("/identity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIdentityStats(
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

        long totalUsers = queryLong("SELECT COUNT(*) FROM users WHERE deleted_at IS NULL");
        long newUsersInRange = queryLong(
                "SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND DATE(created_at) BETWEEN ? AND ?",
                fromDate.toString(), toDate.toString()
        );
        long pendingKyc = queryLong(
                "SELECT COUNT(DISTINCT user_id) FROM (" +
                "  SELECT user_id FROM user_verifications WHERE status = 'PENDING' " +
                "  UNION " +
                "  SELECT user_id FROM face_verifications WHERE status = 'PENDING'" +
                ") AS pending_kyc_users"
        );

        Map<String, Long> usersByRole = new LinkedHashMap<>();
        List<Map<String, Object>> roleRows = jdbcTemplate.queryForList(
                "SELECT r.name AS role_name, COUNT(DISTINCT ur.user_id) AS cnt " +
                "FROM roles r LEFT JOIN user_roles ur ON ur.role_id = r.id " +
                "LEFT JOIN users u ON u.id = ur.user_id AND u.deleted_at IS NULL " +
                "GROUP BY r.name ORDER BY r.name"
        );
        for (Map<String, Object> row : roleRows) {
            usersByRole.put(String.valueOf(row.get("role_name")), ((Number) row.get("cnt")).longValue());
        }

        List<Map<String, Object>> dailyRegistrations = new ArrayList<>();
        List<Map<String, Object>> tsRows = jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS bucket, COUNT(*) AS cnt " +
                "FROM users WHERE deleted_at IS NULL AND DATE(created_at) BETWEEN ? AND ? " +
                "GROUP BY bucket ORDER BY bucket",
                fromDate.toString(), toDate.toString()
        );
        for (Map<String, Object> row : tsRows) {
            dailyRegistrations.add(Map.of(
                    "date", String.valueOf(row.get("bucket")),
                    "count", ((Number) row.get("cnt")).longValue()
            ));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("totalUsers", totalUsers);
        payload.put("newUsersInRange", newUsersInRange);
        payload.put("pendingKyc", pendingKyc);
        payload.put("usersByRole", usersByRole);
        payload.put("dailyRegistrations", dailyRegistrations);
        return ResponseEntity.ok(ApiResponse.success(payload));
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
