package vn.cozynest.admin.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.admin.application.port.DisputePersistencePort;
import vn.cozynest.admin.application.service.DisputeService;
import vn.cozynest.common.dto.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DisputePersistencePort.DisputeDetailView>>> listDisputes(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Authentication auth
    ) {
        String actorId = String.valueOf(auth.getPrincipal());
        return ResponseEntity.ok(ApiResponse.success(
                disputeService.listDisputes(actorId, isStaff(auth), status, search)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DisputePersistencePort.DisputeDetailView>> getDetail(
            @PathVariable String id,
            Authentication auth
    ) {
        String actorId = String.valueOf(auth.getPrincipal());
        return ResponseEntity.ok(ApiResponse.success(
                disputeService.getDetail(id, actorId, isStaff(auth))));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DisputePersistencePort.DisputeDetailView>> createDispute(
            @RequestBody DisputeService.CreateDisputeCommand command,
            Authentication auth,
            HttpServletRequest req
    ) {
        String actorId = String.valueOf(auth.getPrincipal());
        String role = primaryRole(auth);
        return ResponseEntity.ok(ApiResponse.success("Da tao ho so tranh chap",
                disputeService.createDispute(actorId, role, command, extractIp(req), req.getHeader("User-Agent"))));
    }

    @PostMapping("/{id}/events")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DisputePersistencePort.DisputeDetailView>> addComment(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth,
            HttpServletRequest req
    ) {
        String actorId = String.valueOf(auth.getPrincipal());
        String msg = body != null ? body.get("message") : null;
        return ResponseEntity.ok(ApiResponse.success("Da gui phan hoi hoa giai",
                disputeService.addComment(id, actorId, primaryRole(auth), isStaff(auth), msg, extractIp(req), req.getHeader("User-Agent"))));
    }

    @PostMapping("/{id}/mediate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponse<DisputePersistencePort.DisputeDetailView>> mediate(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body,
            Authentication auth,
            HttpServletRequest req
    ) {
        String adminId = String.valueOf(auth.getPrincipal());
        String note = body != null && body.get("note") != null ? String.valueOf(body.get("note")) : null;
        boolean freezeEscrow = body != null && Boolean.TRUE.equals(body.get("freezeEscrow"));
        return ResponseEntity.ok(ApiResponse.success("Da tiep nhan hoa giai tranh chap",
                disputeService.mediate(id, adminId, note, freezeEscrow, extractIp(req), req.getHeader("User-Agent"))));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponse<DisputePersistencePort.DisputeDetailView>> resolve(
            @PathVariable String id,
            @RequestBody DisputeService.ResolveDisputeCommand command,
            Authentication auth,
            HttpServletRequest req
    ) {
        String adminId = String.valueOf(auth.getPrincipal());
        return ResponseEntity.ok(ApiResponse.success("Da giai quyet va ra phan quyet tranh chap",
                disputeService.resolve(id, adminId, command, extractIp(req), req.getHeader("User-Agent"))));
    }

    private boolean isStaff(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_MODERATOR".equals(a.getAuthority()));
    }

    private String primaryRole(Authentication auth) {
        if (auth == null || auth.getAuthorities().isEmpty()) return "USER";
        return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String fwd = request.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : request.getRemoteAddr();
    }
}
