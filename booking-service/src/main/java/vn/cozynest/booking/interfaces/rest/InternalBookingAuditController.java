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
import org.springframework.web.bind.annotation.RestController;
import vn.cozynest.common.dto.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/contracts")
public class InternalBookingAuditController {

    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final JdbcTemplate jdbcTemplate;
    private final String expectedServiceKey;

    @Autowired
    public InternalBookingAuditController(
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
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBookingAuditEvents(
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey
    ) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Truy cap noi bo khong hop le", "ERR_UNAUTHORIZED"));
        }
        List<Map<String, Object>> events = new ArrayList<>();
        try {
            List<Map<String, Object>> sigs = jdbcTemplate.queryForList(
                    "SELECT id, contract_id, signer_id, signer_role, otp_verified, ip_address, user_agent, signed_at " +
                    "FROM contract_signature_audits ORDER BY signed_at DESC LIMIT 50"
            );
            for (Map<String, Object> r : sigs) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", "csig-" + r.get("id"));
                item.put("actor", String.valueOf(r.getOrDefault("signer_id", "unknown")));
                item.put("actorRole", String.valueOf(r.getOrDefault("signer_role", "USER")));
                item.put("action", "CONTRACT_SIGNED");
                item.put("target", "CONTRACT (" + r.getOrDefault("contract_id", "") + ")");
                item.put("details", "otpVerified=" + r.get("otp_verified") + ", ua=" + r.getOrDefault("user_agent", ""));
                item.put("ip", String.valueOf(r.getOrDefault("ip_address", "127.0.0.1")));
                item.put("source", "BOOKING");
                item.put("timestamp", formatTimestamp(r.get("signed_at")));
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
