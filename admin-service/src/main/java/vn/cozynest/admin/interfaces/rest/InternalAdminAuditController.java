package vn.cozynest.admin.interfaces.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.admin.application.service.AdminAuditService;
import vn.cozynest.common.dto.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/internal/admin")
public class InternalAdminAuditController {

    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final AdminAuditService auditService;
    private final String expectedServiceKey;

    @Autowired
    public InternalAdminAuditController(
            AdminAuditService auditService,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String expectedServiceKey,
            @Autowired(required = false) Environment env
    ) {
        this.auditService = auditService;
        this.expectedServiceKey = expectedServiceKey;
        if (env != null && env.matchesProfiles("prod", "production", "staging")) {
            if (DEFAULT_INTERNAL_KEY.equals(expectedServiceKey)) {
                throw new IllegalStateException("Restricted environment requires explicit app.internal.service-key");
            }
        }
    }

    @PostMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recordAudit(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey
    ) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Truy cap noi bo khong hop le", "ERR_UNAUTHORIZED"));
        }
        auditService.record(
                body.get("actorId"),
                body.get("actorRole"),
                body.get("action"),
                body.get("entityType"),
                body.get("entityId"),
                body.get("ipAddress"),
                body.get("userAgent"),
                body.get("payloadDiffJson")
        );
        return ResponseEntity.ok(ApiResponse.success(Map.of("recorded", true)));
    }

    private boolean isKeyValid(String serviceKey) {
        if (expectedServiceKey == null) return false;
        return MessageDigest.isEqual(
                serviceKey.getBytes(StandardCharsets.UTF_8),
                expectedServiceKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
