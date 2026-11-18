package vn.cozynest.admin.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.admin.application.service.AdminAuditService;
import vn.cozynest.common.dto.ApiResponse;

import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/support")
public class AdminSupportTicketController {

    private final JdbcTemplate jdbcTemplate;
    private final Optional<AdminAuditService> auditService;

    public AdminSupportTicketController(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Optional.empty());
    }

    @Autowired
    public AdminSupportTicketController(JdbcTemplate jdbcTemplate, Optional<AdminAuditService> auditService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService != null ? auditService : Optional.empty();
    }

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        List<Map<String, Object>> tickets = new ArrayList<>();
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT t.id, t.ticket_code, t.user_id, t.user_name, t.user_email, t.user_phone, " +
                    "t.subject, t.category, t.priority, t.status, t.description, t.assigned_to, t.assigned_agent_name, " +
                    "t.created_at, t.updated_at, t.resolved_at, " +
                    "(SELECT COUNT(*) FROM cozynest_communication.support_ticket_replies r WHERE r.ticket_id = t.id) AS reply_count " +
                    "FROM cozynest_communication.support_tickets t WHERE 1=1 "
            );
            List<Object> params = new ArrayList<>();
            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                sql.append("AND t.status = ? ");
                params.add(status.toUpperCase());
            }
            if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
                sql.append("AND t.category = ? ");
                params.add(category);
            }
            if (search != null && !search.isBlank()) {
                sql.append("AND (t.ticket_code LIKE ? OR t.subject LIKE ? OR t.user_name LIKE ? OR t.user_email LIKE ?) ");
                String kw = "%" + search.trim() + "%";
                params.add(kw);
                params.add(kw);
                params.add(kw);
                params.add(kw);
            }
            sql.append("ORDER BY t.created_at DESC");

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            for (Map<String, Object> r : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", String.valueOf(r.get("id")));
                item.put("ticketCode", String.valueOf(r.getOrDefault("ticket_code", "")));
                item.put("userId", String.valueOf(r.getOrDefault("user_id", "")));
                item.put("userName", String.valueOf(r.getOrDefault("user_name", "")));
                item.put("userEmail", String.valueOf(r.getOrDefault("user_email", "")));
                item.put("userPhone", r.get("user_phone") != null ? String.valueOf(r.get("user_phone")) : "");
                item.put("subject", String.valueOf(r.getOrDefault("subject", "")));
                item.put("category", String.valueOf(r.getOrDefault("category", "OTHER")));
                item.put("priority", String.valueOf(r.getOrDefault("priority", "MEDIUM")));
                item.put("status", String.valueOf(r.getOrDefault("status", "OPEN")));
                item.put("description", String.valueOf(r.getOrDefault("description", "")));
                item.put("assignedTo", r.get("assigned_to") != null ? String.valueOf(r.get("assigned_to")) : null);
                item.put("assignedAgentName", r.get("assigned_agent_name") != null ? String.valueOf(r.get("assigned_agent_name")) : null);
                item.put("replyCount", ((Number) r.getOrDefault("reply_count", 0)).intValue());
                item.put("createdAt", String.valueOf(r.getOrDefault("created_at", "")));
                item.put("updatedAt", String.valueOf(r.getOrDefault("updated_at", "")));
                item.put("resolvedAt", r.get("resolved_at") != null ? String.valueOf(r.get("resolved_at")) : null);
                tickets.add(item);
            }
        } catch (Exception e) {
            // fallback
        }
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTicketDetail(@PathVariable String id) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, ticket_code, user_id, user_name, user_email, user_phone, subject, category, priority, status, description, assigned_to, assigned_agent_name, created_at, updated_at, resolved_at " +
                    "FROM cozynest_communication.support_tickets WHERE id = ? OR ticket_code = ?",
                    id, id
            );
            if (rows.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Map<String, Object> r = rows.get(0);
            Map<String, Object> detail = new LinkedHashMap<>();
            String realId = String.valueOf(r.get("id"));
            detail.put("id", realId);
            detail.put("ticketCode", String.valueOf(r.getOrDefault("ticket_code", "")));
            detail.put("userId", String.valueOf(r.getOrDefault("user_id", "")));
            detail.put("userName", String.valueOf(r.getOrDefault("user_name", "")));
            detail.put("userEmail", String.valueOf(r.getOrDefault("user_email", "")));
            detail.put("userPhone", r.get("user_phone") != null ? String.valueOf(r.get("user_phone")) : "");
            detail.put("subject", String.valueOf(r.getOrDefault("subject", "")));
            detail.put("category", String.valueOf(r.getOrDefault("category", "OTHER")));
            detail.put("priority", String.valueOf(r.getOrDefault("priority", "MEDIUM")));
            detail.put("status", String.valueOf(r.getOrDefault("status", "OPEN")));
            detail.put("description", String.valueOf(r.getOrDefault("description", "")));
            detail.put("assignedTo", r.get("assigned_to") != null ? String.valueOf(r.get("assigned_to")) : null);
            detail.put("assignedAgentName", r.get("assigned_agent_name") != null ? String.valueOf(r.get("assigned_agent_name")) : null);
            detail.put("createdAt", String.valueOf(r.getOrDefault("created_at", "")));
            detail.put("updatedAt", String.valueOf(r.getOrDefault("updated_at", "")));
            detail.put("resolvedAt", r.get("resolved_at") != null ? String.valueOf(r.get("resolved_at")) : null);

            List<Map<String, Object>> replyRows = jdbcTemplate.queryForList(
                    "SELECT id, ticket_id, sender_id, sender_name, sender_role, message, created_at " +
                    "FROM cozynest_communication.support_ticket_replies WHERE ticket_id = ? ORDER BY created_at ASC",
                    realId
            );
            List<Map<String, Object>> replies = new ArrayList<>();
            for (Map<String, Object> rep : replyRows) {
                Map<String, Object> replyItem = new LinkedHashMap<>();
                replyItem.put("id", String.valueOf(rep.get("id")));
                replyItem.put("ticketId", String.valueOf(rep.get("ticket_id")));
                replyItem.put("senderId", String.valueOf(rep.get("sender_id")));
                replyItem.put("senderName", String.valueOf(rep.getOrDefault("sender_name", "")));
                replyItem.put("senderRole", String.valueOf(rep.getOrDefault("sender_role", "USER")));
                replyItem.put("message", String.valueOf(rep.getOrDefault("message", "")));
                replyItem.put("createdAt", String.valueOf(rep.getOrDefault("created_at", "")));
                replies.add(replyItem);
            }
            detail.put("replies", replies);

            return ResponseEntity.ok(ApiResponse.success(detail));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/tickets/{id}/reply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> replyTicket(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth,
            HttpServletRequest req) {
        String message = body != null ? body.get("message") : null;
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String actor = auth != null ? String.valueOf(auth.getPrincipal()) : "u-admin-01";
        String replyId = "str-" + UUID.randomUUID().toString().substring(0, 8);
        String replyTime = java.time.LocalDateTime.now().toString();

        try {
            jdbcTemplate.update(
                    "INSERT INTO cozynest_communication.support_ticket_replies (id, ticket_id, sender_id, sender_name, sender_role, message, created_at) " +
                    "VALUES (?, ?, ?, ?, 'ADMIN', ?, NOW(3))",
                    replyId, id, actor, "Quản Trị Viên (Admin)", message.trim()
            );

            jdbcTemplate.update(
                    "UPDATE cozynest_communication.support_tickets SET updated_at = NOW(3), " +
                    "status = CASE WHEN status = 'OPEN' THEN 'IN_PROGRESS' ELSE status END " +
                    "WHERE id = ? OR ticket_code = ?",
                    id, id
            );

            auditService.ifPresent(a -> a.record(actor, "ADMIN", "SUPPORT_TICKET_REPLY", "SUPPORT_TICKET", id,
                    extractIp(req), req != null ? req.getHeader("User-Agent") : null, "{\"replyId\":\"" + replyId + "\"}"));

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("id", replyId);
            res.put("ticketId", id);
            res.put("senderId", actor);
            res.put("senderName", "Quản Trị Viên (Admin)");
            res.put("senderRole", "ADMIN");
            res.put("message", message.trim());
            res.put("createdAt", replyTime);

            return ResponseEntity.ok(ApiResponse.success(res));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PatchMapping("/tickets/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth,
            HttpServletRequest req) {
        String newStatus = body != null ? body.get("status") : null;
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String statusUpper = newStatus.trim().toUpperCase();
        String actor = auth != null ? String.valueOf(auth.getPrincipal()) : "u-admin-01";

        try {
            if ("RESOLVED".equals(statusUpper) || "CLOSED".equals(statusUpper)) {
                jdbcTemplate.update(
                        "UPDATE cozynest_communication.support_tickets SET status = ?, updated_at = NOW(3), resolved_at = NOW(3) " +
                        "WHERE id = ? OR ticket_code = ?",
                        statusUpper, id, id
                );
            } else {
                jdbcTemplate.update(
                        "UPDATE cozynest_communication.support_tickets SET status = ?, updated_at = NOW(3), resolved_at = NULL " +
                        "WHERE id = ? OR ticket_code = ?",
                        statusUpper, id, id
                );
            }

            auditService.ifPresent(a -> a.record(actor, "ADMIN", "SUPPORT_TICKET_STATUS", "SUPPORT_TICKET", id,
                    extractIp(req), req != null ? req.getHeader("User-Agent") : null, "{\"newStatus\":\"" + statusUpper + "\"}"));

            return ResponseEntity.ok(ApiResponse.message("Đã cập nhật trạng thái phiếu hỗ trợ thành công"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String fwd = request.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : request.getRemoteAddr();
    }
}

