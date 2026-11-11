package vn.cozynest.billing.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.service.MakerCheckerService;
import vn.cozynest.billing.domain.model.MakerCheckerRequest;
import vn.cozynest.common.dto.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing/maker-checker")
@PreAuthorize("hasRole('ADMIN')")
public class MakerCheckerController {
    private final MakerCheckerService makerCheckerService;

    public MakerCheckerController(MakerCheckerService makerCheckerService) {
        this.makerCheckerService = makerCheckerService;
    }

    @PostMapping("/adjustment")
    public ResponseEntity<ApiResponse<MakerCheckerRequest>> createAdjustment(
            @RequestBody LedgerDto.CreateAdjustmentRequest request,
            Authentication authentication) {
        String makerId = requireActor(authentication);
        try {
            MakerCheckerRequest created = makerCheckerService.createAdjustmentRequest(makerId, request);
            return ResponseEntity.ok(ApiResponse.success("Tạo yêu cầu điều chỉnh sổ cái thành công, đang chờ cấp duyệt thứ hai", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "ERR_BAD_REQUEST"));
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<LedgerDto.AdjustmentResponse>>> getRequests(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(makerCheckerService.getAllRequests(status)));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<MakerCheckerRequest>>> getPending() {
        return ResponseEntity.ok(ApiResponse.success(makerCheckerService.getPendingRequests()));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approve(
            @PathVariable String id,
            Authentication authentication) {
        String checkerId = requireActor(authentication);
        try {
            makerCheckerService.approve(id, checkerId);
            return ResponseEntity.ok(ApiResponse.success("Phê duyệt điều chỉnh thành công. Bút toán đã được hạch toán bất biến vào Sổ cái.", Map.of("approved", true)));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "ERR_MAKER_CHECKER_VIOLATION"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi khi duyệt: " + e.getMessage(), "ERR_ADJUSTMENT_FAILED"));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reject(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String checkerId = requireActor(authentication);
        String reason = body != null ? body.getOrDefault("reason", "Từ chối duyệt") : "Từ chối duyệt";
        try {
            makerCheckerService.reject(id, checkerId, reason);
            return ResponseEntity.ok(ApiResponse.success("Đã từ chối yêu cầu điều chỉnh", Map.of("rejected", true)));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "ERR_MAKER_CHECKER_VIOLATION"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi khi từ chối: " + e.getMessage(), "ERR_REJECT_FAILED"));
        }
    }

    private String requireActor(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("Yeu cau danh tinh quan tri vien");
        }
        return String.valueOf(authentication.getPrincipal());
    }
}
