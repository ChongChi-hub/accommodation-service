package vn.cozynest.identity.domain.repository;

import vn.cozynest.identity.domain.model.IdentityAuditLog;

import java.util.List;

public interface IdentityAuditLogRepository {
    IdentityAuditLog save(IdentityAuditLog log);
    List<IdentityAuditLog> findByUserIdOrderByCreatedAtDesc(String userId);
}
