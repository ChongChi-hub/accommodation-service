package vn.cozynest.admin.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.cozynest.admin.application.port.DisputePersistencePort;
import vn.cozynest.admin.application.port.DisputeQueryPort;
import vn.cozynest.admin.domain.model.Dispute;
import vn.cozynest.admin.domain.repository.DisputeRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class JdbcDisputeRepositoryAdapter implements DisputeRepository, DisputeQueryPort, DisputePersistencePort {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcDisputeRepositoryAdapter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Dispute> getDisputeById(String id) {
        return findById(id);
    }

    @Override
    public Optional<Dispute> findById(String id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, dispute_code, contract_id, room_title, complainant_id, defendant_id, " +
                "dispute_type, amount, reason, status, created_at FROM disputes WHERE id = ?", id);
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(mapDomain(rows.get(0)));
    }

    @Override
    public Optional<Dispute> findByDisputeCode(String disputeCode) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, dispute_code, contract_id, room_title, complainant_id, defendant_id, " +
                "dispute_type, amount, reason, status, created_at FROM disputes WHERE dispute_code = ?", disputeCode);
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(mapDomain(rows.get(0)));
    }

    @Override
    public Dispute save(Dispute d) {
        int updated = jdbcTemplate.update(
                "UPDATE disputes SET status = ?, reason = ?, updated_at = NOW(3) WHERE id = ?",
                d.getStatus(), d.getReason(), d.getId());
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO disputes (id, dispute_code, contract_id, room_title, complainant_id, complainant_name, " +
                    "complainant_role, defendant_id, defendant_name, defendant_role, dispute_type, amount, reason, status, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(3), NOW(3))",
                    d.getId() != null ? d.getId() : UUID.randomUUID().toString(),
                    d.getDisputeCode(),
                    d.getContractId(),
                    d.getRoomTitle(),
                    d.getComplainantId(),
                    d.getComplainantId(),
                    "TENANT",
                    d.getDefendantId(),
                    d.getDefendantId(),
                    "LANDLORD",
                    d.getDisputeType(),
                    d.getAmount() != null ? d.getAmount() : 0L,
                    d.getReason(),
                    d.getStatus() != null ? d.getStatus() : "PENDING");
        }
        return d;
    }

    @Override
    public List<DisputeDetailView> findAllViews(String status, String search) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM disputes ORDER BY created_at DESC");
        String normStatus = status != null ? status.trim().toUpperCase() : "";
        String query = search != null ? search.trim().toLowerCase() : "";
        return rows.stream()
                .map(r -> mapView(r, List.of()))
                .filter(v -> normStatus.isEmpty() || "ALL".equals(normStatus) || normStatus.equalsIgnoreCase(v.status()))
                .filter(v -> query.isEmpty()
                        || contains(v.disputeCode(), query)
                        || contains(v.contractId(), query)
                        || contains(v.roomTitle(), query)
                        || contains(v.complainantName(), query)
                        || contains(v.defendantName(), query)
                        || contains(v.reason(), query))
                .toList();
    }

    @Override
    public List<DisputeDetailView> findByParticipantViews(String userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM disputes WHERE complainant_id = ? OR defendant_id = ? ORDER BY created_at DESC",
                userId, userId);
        return rows.stream().map(r -> mapView(r, List.of())).toList();
    }

    @Override
    public Optional<DisputeDetailView> findViewById(String id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM disputes WHERE id = ? OR dispute_code = ?", id, id);
        if (rows.isEmpty()) return Optional.empty();
        Map<String, Object> r = rows.get(0);
        String realId = String.valueOf(r.get("id"));
        List<Map<String, Object>> evRows = jdbcTemplate.queryForList(
                "SELECT id, dispute_id, actor_id, event_type, message, created_at FROM dispute_events " +
                "WHERE dispute_id = ? ORDER BY created_at ASC", realId);
        List<DisputeEventItem> events = evRows.stream()
                .map(e -> new DisputeEventItem(
                        String.valueOf(e.get("id")),
                        String.valueOf(e.get("dispute_id")),
                        String.valueOf(e.get("actor_id")),
                        String.valueOf(e.get("event_type")),
                        String.valueOf(e.get("message")),
                        toInstant(e.get("created_at"))))
                .toList();
        return Optional.of(mapView(r, events));
    }

    @Override
    public void insertDispute(DisputeDetailView d) {
        String evJson = "[]";
        try {
            evJson = objectMapper.writeValueAsString(d.evidenceUrls() != null ? d.evidenceUrls() : List.of());
        } catch (Exception ignored) {}
        jdbcTemplate.update(
                "INSERT INTO disputes (id, dispute_code, contract_id, room_title, complainant_id, complainant_name, " +
                "complainant_role, defendant_id, defendant_name, defendant_role, dispute_type, amount, reason, " +
                "evidence_urls_json, status, mediator_id, verdict, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(3), NOW(3))",
                d.id(), d.disputeCode(), d.contractId(), d.roomTitle(),
                d.complainantId(), d.complainantName(), d.complainantRole(),
                d.defendantId(), d.defendantName(), d.defendantRole(),
                d.disputeType(), d.amount(), d.reason(),
                evJson, d.status(), d.mediatorId(), d.verdict());
    }

    @Override
    public void updateDisputeState(String id, String status, String mediatorId, String verdict, boolean resolved) {
        if (resolved) {
            jdbcTemplate.update(
                    "UPDATE disputes SET status = ?, mediator_id = COALESCE(?, mediator_id), verdict = ?, resolved_at = NOW(3), updated_at = NOW(3) WHERE id = ?",
                    status, mediatorId, verdict, id);
        } else {
            jdbcTemplate.update(
                    "UPDATE disputes SET status = ?, mediator_id = COALESCE(?, mediator_id), updated_at = NOW(3) WHERE id = ?",
                    status, mediatorId, id);
        }
    }

    @Override
    public void addEvent(String disputeId, String actorId, String eventType, String message) {
        jdbcTemplate.update(
                "INSERT INTO dispute_events (id, dispute_id, actor_id, event_type, message, created_at) VALUES (?, ?, ?, ?, ?, NOW(3))",
                UUID.randomUUID().toString(), disputeId, actorId, eventType, message);
    }

    private Dispute mapDomain(Map<String, Object> r) {
        Instant created = toInstant(r.get("created_at"));
        return Dispute.builder()
                .id(String.valueOf(r.get("id")))
                .disputeCode(String.valueOf(r.get("dispute_code")))
                .contractId(String.valueOf(r.get("contract_id")))
                .roomTitle(String.valueOf(r.get("room_title")))
                .complainantId(String.valueOf(r.get("complainant_id")))
                .defendantId(String.valueOf(r.get("defendant_id")))
                .disputeType(String.valueOf(r.get("dispute_type")))
                .amount(r.get("amount") instanceof Number n ? n.longValue() : 0L)
                .reason(String.valueOf(r.get("reason")))
                .status(String.valueOf(r.get("status")))
                .createdAt(created != null ? LocalDateTime.ofInstant(created, ZoneOffset.UTC) : LocalDateTime.now())
                .build();
    }

    private DisputeDetailView mapView(Map<String, Object> r, List<DisputeEventItem> events) {
        List<String> evidence = new ArrayList<>();
        Object rawEv = r.get("evidence_urls_json");
        if (rawEv != null) {
            try {
                evidence = objectMapper.readValue(String.valueOf(rawEv), new TypeReference<>() {});
            } catch (Exception ignored) {}
        }
        return new DisputeDetailView(
                String.valueOf(r.get("id")),
                String.valueOf(r.get("dispute_code")),
                String.valueOf(r.get("contract_id")),
                String.valueOf(r.get("room_title")),
                String.valueOf(r.get("complainant_id")),
                String.valueOf(r.getOrDefault("complainant_name", r.get("complainant_id"))),
                String.valueOf(r.getOrDefault("complainant_role", "TENANT")),
                String.valueOf(r.get("defendant_id")),
                String.valueOf(r.getOrDefault("defendant_name", r.get("defendant_id"))),
                String.valueOf(r.getOrDefault("defendant_role", "LANDLORD")),
                String.valueOf(r.get("dispute_type")),
                r.get("amount") instanceof Number n ? n.longValue() : 0L,
                String.valueOf(r.get("reason")),
                evidence,
                String.valueOf(r.get("status")),
                r.get("mediator_id") != null ? String.valueOf(r.get("mediator_id")) : null,
                r.get("verdict") != null ? String.valueOf(r.get("verdict")) : null,
                toInstant(r.get("resolved_at")),
                toInstant(r.get("created_at")),
                toInstant(r.get("updated_at")),
                events
        );
    }

    private Instant toInstant(Object val) {
        if (val instanceof Timestamp ts) return ts.toInstant();
        if (val instanceof LocalDateTime ldt) return ldt.toInstant(ZoneOffset.UTC);
        return null;
    }

    private boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle);
    }
}
