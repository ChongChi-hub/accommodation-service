package vn.cozynest.billing.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.service.MakerCheckerService;
import vn.cozynest.billing.domain.model.MakerCheckerRequest;
import vn.cozynest.common.dto.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing/maker-checker")
public class MakerCheckerController {
    private final MakerCheckerService makerCheckerService;

    public MakerCheckerController(MakerCheckerService makerCheckerService) {
        this.makerCheckerService = makerCheckerService;
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<MakerCheckerRequest>>> getPending() {
        List<MakerCheckerRequest> list = makerCheckerService.getPendingRequests();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approve(
            @PathVariable String id,
            Authentication authentication) {
        String checkerId = (String) authentication.getPrincipal();
        try {
            makerCheckerService.approve(id, checkerId);
            return ResponseEntity.ok(ApiResponse.success("Phê duyệt điều chỉnh thành công", Map.of("approved", true)));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "ERR_MAKER_CHECKER_VIOLATION"));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reject(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String checkerId = (String) authentication.getPrincipal();
        String reason = body != null ? body.getOrDefault("reason", "Từ chối duyệt") : "Từ chối duyệt";
        try {
            makerCheckerService.reject(id, checkerId, reason);
            return ResponseEntity.ok(ApiResponse.success("Đã từ chối yêu cầu", Map.of("rejected", true)));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "ERR_MAKER_CHECKER_VIOLATION"));
        }
    }
}

