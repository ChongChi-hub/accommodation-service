package vn.cozynest.billing.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.dto.RefundDto;
import vn.cozynest.billing.application.service.RefundService;
import vn.cozynest.common.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    public ResponseEntity<ApiResponse<RefundDto.RefundResponse>> requestRefund(
            @Valid @RequestBody RefundDto.CreateRefundCommand command,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Da tiep nhan yeu cau hoan tien",
                refundService.requestRefund((String) auth.getPrincipal(), command)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundDto.RefundResponse>> approveRefund(
            @PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Da duyet va giai ngan hoan tien",
                refundService.approveRefund(id, (String) auth.getPrincipal())));
    }

    @GetMapping("/tenant")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<RefundDto.RefundResponse>>> getTenantRefunds(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(refundService.getTenantRefunds((String) auth.getPrincipal())));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<RefundDto.RefundResponse>>> getAllRefunds() {
        return ResponseEntity.ok(ApiResponse.success(refundService.getAllRefunds()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RefundDto.RefundResponse>> getRefund(@PathVariable String id, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
        return ResponseEntity.ok(ApiResponse.success(refundService.getRefundById(id, (String) auth.getPrincipal(), isAdmin)));
    }
}

