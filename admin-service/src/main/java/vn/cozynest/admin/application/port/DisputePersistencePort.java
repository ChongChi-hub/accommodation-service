package vn.cozynest.admin.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DisputePersistencePort {

    record DisputeEventItem(
            String id,
            String disputeId,
            String actorId,
            String eventType,
            String message,
            Instant createdAt
    ) {}

    record DisputeDetailView(
            String id,
            String disputeCode,
            String contractId,
            String roomTitle,
            String complainantId,
            String complainantName,
            String complainantRole,
            String defendantId,
            String defendantName,
            String defendantRole,
            String disputeType,
            long amount,
            String reason,
            List<String> evidenceUrls,
            String status,
            String mediatorId,
            String verdict,
            Instant resolvedAt,
            Instant createdAt,
            Instant updatedAt,
            List<DisputeEventItem> events
    ) {}

    List<DisputeDetailView> findAllViews(String status, String search);

    List<DisputeDetailView> findByParticipantViews(String userId);

    Optional<DisputeDetailView> findViewById(String id);

    void insertDispute(DisputeDetailView d);

    void updateDisputeState(String id, String status, String mediatorId, String verdict, boolean resolved);

    void addEvent(String disputeId, String actorId, String eventType, String message);
}
