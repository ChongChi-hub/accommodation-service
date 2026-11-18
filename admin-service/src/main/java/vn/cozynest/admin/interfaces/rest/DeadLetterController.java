package vn.cozynest.admin.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.admin.application.service.AdminAuditService;
import vn.cozynest.admin.application.service.DeadLetterAdminService;
import vn.cozynest.admin.domain.model.DeadLetterMessage;
import vn.cozynest.common.dto.ApiResponse;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/dlq")
@PreAuthorize("hasRole('ADMIN')")
public class DeadLetterController {

    private final DeadLetterAdminService dlqService;
    private final Optional<AdminAuditService> auditService;

    public DeadLetterController(DeadLetterAdminService dlqService) {
        this(dlqService, Optional.empty());
    }

    @Autowired
    public DeadLetterController(DeadLetterAdminService dlqService, Optional<AdminAuditService> auditService) {
        this.dlqService = dlqService;
        this.auditService = auditService != null ? auditService : Optional.empty();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeadLetterMessage>>> list() {
        return ResponseEntity.ok(ApiResponse.success(dlqService.listAll()));
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> metrics() {
        return ResponseEntity.ok(ApiResponse.success(dlqService.getMetrics()));
    }

    @PostMapping("/{id}/replay")
    public ResponseEntity<ApiResponse<DeadLetterMessage>> replay(
            @PathVariable String id,
            Authentication auth,
            HttpServletRequest req
    ) {
        String adminId = auth != null ? auth.getName() : "admin";
        DeadLetterMessage msg = dlqService.replay(id, adminId);
        auditService.ifPresent(a -> a.record(adminId, "ADMIN", "DLQ_REPLAY", "DEAD_LETTER", id,
                req != null ? req.getRemoteAddr() : "127.0.0.1", req != null ? req.getHeader("User-Agent") : null, null));
        return ResponseEntity.ok(ApiResponse.success("Da xep hang phat lai su kien", msg));
    }

    @PostMapping("/{id}/discard")
    public ResponseEntity<ApiResponse<DeadLetterMessage>> discard(
            @PathVariable String id,
            Authentication auth,
            HttpServletRequest req
    ) {
        String adminId = auth != null ? auth.getName() : "admin";
        DeadLetterMessage msg = dlqService.discard(id, adminId);
        auditService.ifPresent(a -> a.record(adminId, "ADMIN", "DLQ_DISCARD", "DEAD_LETTER", id,
                req != null ? req.getRemoteAddr() : "127.0.0.1", req != null ? req.getHeader("User-Agent") : null, null));
        return ResponseEntity.ok(ApiResponse.success("Da danh dau loai bo su kien DLQ", msg));
    }
}

