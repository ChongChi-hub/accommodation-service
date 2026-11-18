package vn.cozynest.admin.application.port;

import java.util.List;
import java.util.Map;

public interface AdminAuditPort {
    void insertAuditLog(String actorId, String actorRole, String action, String entityType,
                        String entityId, String ipAddress, String userAgent, String payloadDiffJson);

    List<Map<String, Object>> findLocalAuditLogs(int limit);

    List<Map<String, Object>> findDisputeEventAuditLogs(int limit);

    List<Map<String, Object>> fetchCrossServiceAuditEvents();
}
