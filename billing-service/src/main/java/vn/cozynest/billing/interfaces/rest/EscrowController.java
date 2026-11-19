package vn.cozynest.billing.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.service.EscrowService;
import vn.cozynest.billing.domain.model.EscrowHold;
import vn.cozynest.common.dto.ApiResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing/escrow")
public class EscrowController {
    private final EscrowService escrowService;

    public EscrowController(EscrowService escrowService) {
        this.escrowService = escrowService;
    }

    @PostMapping("/init")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EscrowHold>> initEscrowHold(@RequestBody Map<String, Object> body) {
        String contractId = String.valueOf(body.getOrDefault("contractId", ""));
        String tenantId = String.valueOf(body.getOrDefault("tenantId", ""));
        String landlordId = String.valueOf(body.getOrDefault("landlordId", ""));
        long amount = body.get("amount") instanceof Number n ? n.longValue() : 0L;
        EscrowHold hold = escrowService.openHold(contractId, tenantId, landlordId, amount, 72);
        return ResponseEntity.ok(ApiResponse.success("Khoi tao khoan ky quy thanh cong", hold));
    }

    @GetMapping("/tenant")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<EscrowHold>>> getTenantEscrowHolds(Authentication auth) {
        String tenantId = (String) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(escrowService.getHoldsForTenant(tenantId)));
    }

    @GetMapping("/landlord")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<EscrowHold>>> getLandlordEscrowHolds(Authentication auth) {
        String landlordId = (String) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(escrowService.getHoldsForLandlord(landlordId)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<EscrowHold>>> getAllEscrowHolds() {
        return ResponseEntity.ok(ApiResponse.success(escrowService.getAllHolds()));
    }

    @GetMapping("/{contractId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EscrowHold>> getEscrowHold(
            @PathVariable String contractId,
            Authentication auth) {
        EscrowHold hold = escrowService.findHoldForParticipant(contractId, (String) auth.getPrincipal(), isAdmin(auth))
                .orElse(null);
        return ResponseEntity.ok(ApiResponse.success(hold));
    }

    @PostMapping("/{contractId}/confirm-move-in")
    @PreAuthorize("hasAnyRole('TENANT', 'LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<EscrowHold>> confirmMoveIn(
            @PathVariable String contractId,
            Authentication auth) {
        EscrowHold updated = escrowService.confirmMoveIn(contractId, (String) auth.getPrincipal(), isAdmin(auth));
        return ResponseEntity.ok(ApiResponse.success("Da xac nhan nhan phong ky quy", updated));
    }

    @PostMapping("/{contractId}/freeze")
    @PreAuthorize("hasAnyRole('TENANT', 'LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<EscrowHold>> freezeForDispute(
            @PathVariable String contractId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        String reason = body != null ? body.getOrDefault("reason", "Tranh chap hop dong") : "Tranh chap hop dong";
        EscrowHold updated = escrowService.freezeForDispute(contractId, (String) auth.getPrincipal(), isAdmin(auth), reason);
        return ResponseEntity.ok(ApiResponse.success("Da dong bang khoan ky quy do tranh chap", updated));
    }

    @PostMapping("/{contractId}/reject-proof")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<EscrowHold>> rejectDepositProof(
            @PathVariable String contractId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        String reason = body != null ? body.getOrDefault("reason", "Biên lai hoặc mã giao dịch không hợp lệ") : "Biên lai hoặc mã giao dịch không hợp lệ";
        EscrowHold updated = escrowService.rejectDepositProof(contractId, (String) auth.getPrincipal(), isAdmin(auth), reason);
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối biên lai cọc không hợp lệ và yêu cầu khách thuê nạp lại", updated));
    }

    @PostMapping("/{contractId}/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EscrowHold>> unfreezeDispute(@PathVariable String contractId) {
        return ResponseEntity.ok(ApiResponse.success("Da go dong bang khoan ky quy",
                escrowService.unfreezeDispute(contractId)));
    }

    @PostMapping("/{contractId}/release")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EscrowHold>> releaseDeposit(
            @PathVariable String contractId,
            Authentication auth) {
        EscrowHold hold = escrowService.getHoldForParticipant(contractId, (String) auth.getPrincipal(), true);
        if (!hold.isBothConfirmedMoveIn()) {
            throw new IllegalStateException("Chưa đủ 2 điều kiện giải ngân: Chủ trọ phải bấm Bàn giao phòng và Khách thuê phải bấm Xác nhận nhận phòng.");
        }
        escrowService.disburseDeposit(contractId, hold.getLandlordId(), hold.getAmount(), (String) auth.getPrincipal());
        return ResponseEntity.ok(ApiResponse.success("Da giai ngan tien coc cho chu tro",
                escrowService.getHoldForParticipant(contractId, (String) auth.getPrincipal(), true)));
    }

    @PostMapping("/{contractId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EscrowHold>> refundDeposit(
            @PathVariable String contractId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        String reason = body != null ? body.getOrDefault("reason", "Hoan coc theo quyet dinh Admin") : "Hoan coc theo quyet dinh Admin";
        EscrowHold hold = escrowService.getHoldForParticipant(contractId, (String) auth.getPrincipal(), true);
        escrowService.refundDeposit(contractId, hold.getTenantId(), hold.getEffectiveRetainedAmount(), reason, (String) auth.getPrincipal());
        return ResponseEntity.ok(ApiResponse.success("Da hoan tien coc cho nguoi thue",
                escrowService.getHoldForParticipant(contractId, (String) auth.getPrincipal(), true)));
    }

    @PostMapping("/{contractId}/deduction-claim")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<EscrowHold>> submitDeductionClaim(
            @PathVariable String contractId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        long claimAmount = body.get("claimAmount") instanceof Number n ? n.longValue() : 0L;
        String reason = String.valueOf(body.getOrDefault("reason", ""));
        String proofUrl = body.get("proofUrl") != null ? String.valueOf(body.get("proofUrl")) : null;
        EscrowHold updated = escrowService.submitDeductionClaim(contractId, (String) auth.getPrincipal(), claimAmount, reason, proofUrl);
        return ResponseEntity.ok(ApiResponse.success("Da gui de nghi khau tru coc cho Admin xet duyet", updated));
    }

    @PostMapping("/{contractId}/deduction-decision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EscrowHold>> decideDeductionClaim(
            @PathVariable String contractId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        String note = String.valueOf(body.getOrDefault("note", approved ? "Phe duyet khau tru" : "Tu choi khau tru"));
        EscrowHold updated = escrowService.decideDeductionClaim(contractId, (String) auth.getPrincipal(), approved, note);
        return ResponseEntity.ok(ApiResponse.success("Da cap nhat quyet dinh khau tru coc", updated));
    }

    @PostMapping("/settle-pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> settlePendingReleases() {
        int count = escrowService.settleMaturedPendingReleases(java.time.Instant.now());
        return ResponseEntity.ok(ApiResponse.success("Da quet chuyen coc het thoi gian cho sang so du kha dung", Map.of("settledCount", count)));
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
    }
}

