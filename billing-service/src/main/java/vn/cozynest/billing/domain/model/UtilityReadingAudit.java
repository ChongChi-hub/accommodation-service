package vn.cozynest.billing.domain.model;

import java.time.Instant;
import java.util.UUID;

public record UtilityReadingAudit(
        String id,
        String readingId,
        String action,
        String prevSnapshotJson,
        String actorId,
        String reason,
        Instant createdAt
) {
    public static UtilityReadingAudit of(String readingId, String action, String prevSnapshotJson, String actorId, String reason) {
        return new UtilityReadingAudit(UUID.randomUUID().toString(), readingId, action, prevSnapshotJson, actorId, reason, Instant.now());
    }
}

