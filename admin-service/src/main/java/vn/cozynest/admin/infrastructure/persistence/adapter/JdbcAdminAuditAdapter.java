package vn.cozynest.admin.infrastructure.persistence.adapter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.cozynest.admin.application.port.AdminAuditPort;
import vn.cozynest.admin.infrastructure.client.HttpCrossServiceAuditAdapter;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JdbcAdminAuditAdapter implements AdminAuditPort {

    private final JdbcTemplate jdbcTemplate;
    private final HttpCrossServiceAuditAdapter crossServiceAuditAdapter;

    public JdbcAdminAuditAdapter(JdbcTemplate jdbcTemplate, HttpCrossServiceAuditAdapter crossServiceAuditAdapter) {
        this.jdbcTemplate = jdbcTemplate;
        this.crossServiceAuditAdapter = crossServiceAuditAdapter;
    }

    @Override
    public void insertAuditLog(String actorId, String actorRole, String action, String entityType,
                               String entityId, String ipAddress, String userAgent, String payloadDiffJson) {
        jdbcTemplate.update(
                "INSERT INTO audit_logs (id, actor_id, actor_role, action, entity_type, entity_id, ip_address, user_agent, payload_diff_json, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(3))",
                UUID.randomUUID().toString(),
                actorId != null && !actorId.isBlank() ? actorId : "SYSTEM",
                actorRole != null && !actorRole.isBlank() ? actorRole : "ADMIN",
                action != null ? action : "ADMIN_ACTION",
                entityType != null ? entityType : "SYSTEM",
                entityId != null ? entityId : "-",
                ipAddress != null ? ipAddress : "127.0.0.1",
                userAgent,
                payloadDiffJson
        );
    }

    @Override
    public List<Map<String, Object>> findLocalAuditLogs(int limit) {
        int cap = Math.max(1, Math.min(limit, 200));
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, actor_id, actor_role, action, entity_type, entity_id, ip_address, user_agent, payload_diff_json, created_at " +
                    "FROM audit_logs ORDER BY created_at DESC LIMIT ?", cap
            );
            for (Map<String, Object> r : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", String.valueOf(r.get("id")));
                item.put("actor", String.valueOf(r.getOrDefault("actor_id", "u-admin-01")));
                item.put("actorRole", String.valueOf(r.getOrDefault("actor_role", "ADMIN")));
                item.put("action", String.valueOf(r.getOrDefault("action", "")));
                item.put("target", r.getOrDefault("entity_type", "") + " (" + r.getOrDefault("entity_id", "") + ")");
                item.put("details", r.get("payload_diff_json") != null ? String.valueOf(r.get("payload_diff_json")) : null);
                item.put("ip", String.valueOf(r.getOrDefault("ip_address", "127.0.0.1")));
                item.put("userAgent", r.get("user_agent") != null ? String.valueOf(r.get("user_agent")) : null);
                item.put("source", "ADMIN");
                item.put("timestamp", formatTs(r.get("created_at")));
                list.add(item);
            }
        } catch (Exception ignored) {}
        return list;
    }

    @Override
    public List<Map<String, Object>> findDisputeEventAuditLogs(int limit) {
        int cap = Math.max(1, Math.min(limit, 100));
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT de.id, de.dispute_id, de.actor_id, de.event_type, de.message, de.created_at, d.dispute_code " +
                    "FROM dispute_events de LEFT JOIN disputes d ON d.id = de.dispute_id " +
                    "ORDER BY de.created_at DESC LIMIT ?", cap
            );
            for (Map<String, Object> r : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", "de-" + r.get("id"));
                item.put("actor", String.valueOf(r.getOrDefault("actor_id", "system")));
                item.put("actorRole", "MEDIATOR");
                item.put("action", "DISPUTE_" + r.getOrDefault("event_type", "EVENT"));
                String code = r.get("dispute_code") != null ? String.valueOf(r.get("dispute_code")) : String.valueOf(r.get("dispute_id"));
                item.put("target", "DISPUTE (" + code + ")");
                item.put("details", r.get("message") != null ? String.valueOf(r.get("message")) : null);
                item.put("ip", "127.0.0.1");
                item.put("source", "DISPUTE");
                item.put("timestamp", formatTs(r.get("created_at")));
                list.add(item);
            }
        } catch (Exception ignored) {}
        return list;
    }

    @Override
    public List<Map<String, Object>> fetchCrossServiceAuditEvents() {
        return crossServiceAuditAdapter.fetchAll();
    }

    private String formatTs(Object val) {
        if (val instanceof Timestamp ts) {
            return ts.toInstant().toString();
        }
        return val != null ? String.valueOf(val) : "";
    }
}
