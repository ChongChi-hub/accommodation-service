package vn.cozynest.admin.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.admin.application.service.AdminAuditService;
import vn.cozynest.admin.application.service.AdminPlatformStatsService;
import vn.cozynest.common.dto.ApiResponse;

import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminDashboardController {

    private final JdbcTemplate jdbcTemplate;
    private final Optional<AdminAuditService> auditService;
    private final Optional<AdminPlatformStatsService> platformStatsService;

    public AdminDashboardController(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Optional.empty(), Optional.empty());
    }

    public AdminDashboardController(JdbcTemplate jdbcTemplate, Optional<AdminAuditService> auditService) {
        this(jdbcTemplate, auditService, Optional.empty());
    }

    @Autowired
    public AdminDashboardController(
            JdbcTemplate jdbcTemplate,
            Optional<AdminAuditService> auditService,
            Optional<AdminPlatformStatsService> platformStatsService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService != null ? auditService : Optional.empty();
        this.platformStatsService = platformStatsService != null ? platformStatsService : Optional.empty();
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        if (platformStatsService.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(platformStatsService.get().getLegacyStats()));
        }
        Map<String, Object> unavailable = new LinkedHashMap<>();
        unavailable.put("degraded", true);
        unavailable.put("totalUsers", null);
        unavailable.put("activeRooms", null);
        unavailable.put("pendingApprovals", null);
        unavailable.put("pendingKyc", null);
        unavailable.put("monthlyRevenue", null);
        return ResponseEntity.ok(ApiResponse.success(unavailable));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getReports() {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT r.id, r.reporter_id, r.target_type, r.target_id, r.reason, r.description, r.evidence_urls_json, r.status, r.created_at, " +
                    "u.full_name AS reporter_name " +
                    "FROM reports r " +
                    "LEFT JOIN cozynest_identity.users u ON r.reporter_id = u.id " +
                    "ORDER BY r.created_at DESC"
            );
            for (Map<String, Object> r : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                String id = String.valueOf(r.get("id"));
                item.put("id", id);
                item.put("targetType", String.valueOf(r.getOrDefault("target_type", "ROOM")));
                String targetId = String.valueOf(r.getOrDefault("target_id", ""));
                item.put("targetId", targetId);
                item.put("targetTitle", "Vi phạm " + r.getOrDefault("target_type", "") + " #" + targetId);
                String repName = r.get("reporter_name") != null ? String.valueOf(r.get("reporter_name")) : String.valueOf(r.getOrDefault("reporter_id", "Người dùng"));
                item.put("reporterName", repName);
                item.put("reason", String.valueOf(r.getOrDefault("reason", "")));
                item.put("description", r.get("description") != null ? String.valueOf(r.get("description")) : "");
                List<String> evidenceList = new ArrayList<>();
                if (r.get("evidence_urls_json") != null) {
                    try {
                        evidenceList = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                                String.valueOf(r.get("evidence_urls_json")),
                                new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}
                        );
                    } catch (Exception ignored) {}
                }
                item.put("evidenceUrls", evidenceList);
                item.put("status", String.valueOf(r.getOrDefault("status", "PENDING")));
                item.put("createdAt", String.valueOf(r.getOrDefault("created_at", "")));
                list.add(item);
            }
        } catch (Exception e) {
            // fallback
        }
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/reports/{id}/resolve")
    public ResponseEntity<ApiResponse<Void>> resolveReport(
            @PathVariable String id,
            Authentication auth,
            HttpServletRequest req) {
        try {
            jdbcTemplate.update("UPDATE reports SET status = 'RESOLVED', resolved_at = NOW(3) WHERE id = ?", id);
        } catch (Exception ignored) {}
        String actor = auth != null ? String.valueOf(auth.getPrincipal()) : "u-admin-01";
        auditService.ifPresent(a -> a.record(actor, "ADMIN", "REPORT_RESOLVE", "REPORT", id,
                extractIp(req), req != null ? req.getHeader("User-Agent") : null, "{\"status\":\"RESOLVED\"}"));
        return ResponseEntity.ok(ApiResponse.message("Đã xử lý báo cáo vi phạm thành công"));
    }

    @PostMapping("/reports/{id}/dismiss")
    public ResponseEntity<ApiResponse<Void>> dismissReport(
            @PathVariable String id,
            Authentication auth,
            HttpServletRequest req) {
        try {
            jdbcTemplate.update("UPDATE reports SET status = 'DISMISSED', resolved_at = NOW(3) WHERE id = ?", id);
        } catch (Exception ignored) {}
        String actor = auth != null ? String.valueOf(auth.getPrincipal()) : "u-admin-01";
        auditService.ifPresent(a -> a.record(actor, "ADMIN", "REPORT_DISMISS", "REPORT", id,
                extractIp(req), req != null ? req.getHeader("User-Agent") : null, "{\"status\":\"DISMISSED\"}"));
        return ResponseEntity.ok(ApiResponse.message("Đã bác bỏ báo cáo vi phạm thành công"));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAuditLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "100") int limit) {
        if (auditService.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(auditService.get().getAuditLogs(search, source, action, limit)));
        }
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, actor_id, actor_role, action, entity_type, entity_id, ip_address, created_at FROM audit_logs ORDER BY created_at DESC LIMIT 50"
            );
            int idx = 1;
            for (Map<String, Object> r : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", idx++);
                item.put("actor", String.valueOf(r.getOrDefault("actor_id", "u-admin-01")));
                item.put("action", String.valueOf(r.getOrDefault("action", "")));
                item.put("target", r.getOrDefault("entity_type", "") + " (" + r.getOrDefault("entity_id", "") + ")");
                item.put("ip", String.valueOf(r.getOrDefault("ip_address", "127.0.0.1")));
                item.put("timestamp", String.valueOf(r.getOrDefault("created_at", "")));
                list.add(item);
            }
        } catch (Exception ignored) {}
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT setting_key, setting_value FROM system_settings");
            for (Map<String, Object> r : rows) {
                String val = String.valueOf(r.get("setting_value"));
                if ("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) {
                    settings.put(String.valueOf(r.get("setting_key")), Boolean.parseBoolean(val));
                } else {
                    settings.put(String.valueOf(r.get("setting_key")), val);
                }
            }
        } catch (Exception ignored) {}
        settings.putIfAbsent("maintenanceMode", false);
        settings.putIfAbsent("vietQrActive", true);
        settings.putIfAbsent("autoApproveRooms", false);
        settings.putIfAbsent("require2FAStaff", true);
        settings.putIfAbsent("sessionTimeoutMinutes", "60");
        settings.putIfAbsent("maxLoginAttempts", "5");
        settings.putIfAbsent("supportHotline", "1900 6868");
        settings.putIfAbsent("commissionRate", "1.5");
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<Void>> updateSettings(
            @RequestBody Map<String, Object> body,
            Authentication auth,
            HttpServletRequest req) {
        if (body != null) {
            for (Map.Entry<String, Object> entry : body.entrySet()) {
                try {
                    jdbcTemplate.update(
                            "INSERT INTO system_settings (id, setting_key, setting_value, updated_at) VALUES (UUID(), ?, ?, NOW(3)) " +
                            "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = NOW(3)",
                            entry.getKey(), String.valueOf(entry.getValue())
                    );
                } catch (Exception ignored) {}
            }
            String actor = auth != null ? String.valueOf(auth.getPrincipal()) : "u-admin-01";
            auditService.ifPresent(a -> a.record(actor, "ADMIN", "SYSTEM_CONFIG_UPDATE", "SYSTEM_SETTING",
                    String.join(",", body.keySet()), extractIp(req), req != null ? req.getHeader("User-Agent") : null, null));
        }
        return ResponseEntity.ok(ApiResponse.message("Da luu cau hinh he thong thanh cong"));
    }

    @PostMapping("/clear-cache")
    public ResponseEntity<ApiResponse<Void>> clearCache(
            Authentication auth,
            HttpServletRequest req) {
        platformStatsService.ifPresent(AdminPlatformStatsService::clearCache);
        String actor = auth != null ? String.valueOf(auth.getPrincipal()) : "u-admin-01";
        auditService.ifPresent(a -> a.record(actor, "ADMIN", "CACHE_CLEAR", "SYSTEM_CACHE",
                "ALL", extractIp(req), req != null ? req.getHeader("User-Agent") : null, "{\"action\":\"CLEAR_CACHE\"}"));
        return ResponseEntity.ok(ApiResponse.message("Đã xóa bộ nhớ đệm hệ thống thành công"));
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String fwd = request.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : request.getRemoteAddr();
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalytics() {
        if (platformStatsService.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(platformStatsService.get().getLegacyAnalytics()));
        }
        Map<String, Object> unavailable = new LinkedHashMap<>();
        unavailable.put("degraded", true);
        unavailable.put("successRate", null);
        unavailable.put("avgTimeToFillDays", null);
        unavailable.put("totalSearches", null);
        unavailable.put("topAreas", List.of());
        return ResponseEntity.ok(ApiResponse.success(unavailable));
    }
}

