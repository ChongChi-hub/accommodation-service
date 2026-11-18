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
import org.springframework.web.bind.annotation.RestController;
import vn.cozynest.common.dto.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/users")
public class InternalIdentityAuditController {

    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final JdbcTemplate jdbcTemplate;
    private final String expectedServiceKey;

    @Autowired
    public InternalIdentityAuditController(
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

    @GetMapping("/audit-events")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getIdentityAuditEvents(
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey
    ) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Truy cap noi bo khong hop le", "ERR_UNAUTHORIZED"));
        }
        List<Map<String, Object>> events = new ArrayList<>();
        try {
            List<Map<String, Object>> kycRows = jdbcTemplate.queryForList(
                    "SELECT id, verification_id, from_status, to_status, actor_id, reason, created_at " +
                    "FROM kyc_status_history ORDER BY created_at DESC LIMIT 50"
            );
            for (Map<String, Object> r : kycRows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", "kyc-" + r.get("id"));
                item.put("actor", String.valueOf(r.getOrDefault("actor_id", "system")));
                item.put("actorRole", "ADMIN_OR_USER");
                item.put("action", "KYC_" + r.getOrDefault("from_status", "") + "_TO_" + r.getOrDefault("to_status", ""));
                item.put("target", "KYC (" + r.getOrDefault("verification_id", "") + ")");
                item.put("details", r.get("reason"));
                item.put("ip", "127.0.0.1");
                item.put("source", "IDENTITY");
                item.put("timestamp", formatTimestamp(r.get("created_at")));
                events.add(item);
            }
        } catch (Exception ignored) {}

        try {
            List<Map<String, Object>> idRows = jdbcTemplate.queryForList(
                    "SELECT id, user_id, actor_id, event_type, ip_address, details, created_at " +
                    "FROM identity_audit_logs ORDER BY created_at DESC LIMIT 50"
            );
            for (Map<String, Object> r : idRows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", "ida-" + r.get("id"));
                item.put("actor", String.valueOf(r.get("actor_id") != null ? r.get("actor_id") : r.getOrDefault("user_id", "system")));
                item.put("actorRole", "IDENTITY");
                item.put("action", String.valueOf(r.getOrDefault("event_type", "IDENTITY_EVENT")));
                item.put("target", "USER (" + r.getOrDefault("user_id", "") + ")");
                item.put("details", r.get("details"));
                item.put("ip", String.valueOf(r.getOrDefault("ip_address", "127.0.0.1")));
                item.put("source", "IDENTITY");
                item.put("timestamp", formatTimestamp(r.get("created_at")));
                events.add(item);
            }
        } catch (Exception ignored) {}

        return ResponseEntity.ok(ApiResponse.success(events));
    }

    private String formatTimestamp(Object val) {
        if (val instanceof java.sql.Timestamp ts) return ts.toInstant().toString();
        return val != null ? String.valueOf(val) : "";
    }

    private boolean isKeyValid(String serviceKey) {
        if (expectedServiceKey == null) return false;
        return MessageDigest.isEqual(
                serviceKey.getBytes(StandardCharsets.UTF_8),
                expectedServiceKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
