package vn.cozynest.admin.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.cozynest.admin.application.port.AdminAuditPort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class AdminAuditService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditService.class);
    private final AdminAuditPort auditPort;

    public AdminAuditService(AdminAuditPort auditPort) {
        this.auditPort = auditPort;
    }

    public void record(String actorId, String actorRole, String action, String entityType,
                       String entityId, String ipAddress, String userAgent, String payloadDiffJson) {
        try {
            auditPort.insertAuditLog(actorId, actorRole, action, entityType, entityId, ipAddress, userAgent, payloadDiffJson);
        } catch (Exception ex) {
            log.warn("Failed to record admin audit log action={} entity={}/{}: {}", action, entityType, entityId, ex.getMessage());
        }
    }

    public List<Map<String, Object>> getAuditLogs(String search, String source, String action, int limit) {
        int cap = Math.max(1, Math.min(limit, 300));
        List<Map<String, Object>> combined = new ArrayList<>();
        combined.addAll(auditPort.findLocalAuditLogs(cap));
        combined.addAll(auditPort.findDisputeEventAuditLogs(cap));
        combined.addAll(auditPort.fetchCrossServiceAuditEvents());

        String normalizedSource = source != null ? source.trim().toUpperCase() : "";
        String normalizedAction = action != null ? action.trim().toUpperCase() : "";
        String query = search != null ? search.trim().toLowerCase() : "";

        return combined.stream()
                .filter(item -> normalizedSource.isEmpty() || "ALL".equals(normalizedSource)
                        || normalizedSource.equalsIgnoreCase(String.valueOf(item.getOrDefault("source", ""))))
                .filter(item -> normalizedAction.isEmpty() || "ALL".equals(normalizedAction)
                        || String.valueOf(item.getOrDefault("action", "")).toUpperCase().contains(normalizedAction))
                .filter(item -> query.isEmpty()
                        || String.valueOf(item.getOrDefault("actor", "")).toLowerCase().contains(query)
                        || String.valueOf(item.getOrDefault("action", "")).toLowerCase().contains(query)
                        || String.valueOf(item.getOrDefault("target", "")).toLowerCase().contains(query)
                        || String.valueOf(item.getOrDefault("details", "")).toLowerCase().contains(query)
                        || String.valueOf(item.getOrDefault("ip", "")).toLowerCase().contains(query))
                .sorted(Comparator.comparing(
                        (Map<String, Object> m) -> String.valueOf(m.getOrDefault("timestamp", "")),
                        Comparator.reverseOrder()))
                .limit(cap)
                .toList();
    }
}
