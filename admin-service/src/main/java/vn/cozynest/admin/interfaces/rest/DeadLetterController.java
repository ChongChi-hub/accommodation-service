package vn.cozynest.admin.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.admin.application.service.DeadLetterAdminService;
import vn.cozynest.admin.domain.model.DeadLetterMessage;
import vn.cozynest.common.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dlq")
@PreAuthorize("hasRole('ADMIN')")
public class DeadLetterController {

    private final DeadLetterAdminService dlqService;

    public DeadLetterController(DeadLetterAdminService dlqService) {
        this.dlqService = dlqService;
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
            Authentication auth
    ) {
        String adminId = auth != null ? auth.getName() : "admin";
        return ResponseEntity.ok(ApiResponse.success("Da xep hang phat lai su kien", dlqService.replay(id, adminId)));
    }

    @PostMapping("/{id}/discard")
    public ResponseEntity<ApiResponse<DeadLetterMessage>> discard(
            @PathVariable String id,
            Authentication auth
    ) {
        String adminId = auth != null ? auth.getName() : "admin";
        return ResponseEntity.ok(ApiResponse.success("Da danh dau loai bo su kien DLQ", dlqService.discard(id, adminId)));
    }
}

