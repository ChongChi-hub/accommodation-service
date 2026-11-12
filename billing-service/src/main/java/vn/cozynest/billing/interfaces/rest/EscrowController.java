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

    @GetMapping("/{contractId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EscrowHold>> getEscrowHold(
            @PathVariable String contractId,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                escrowService.getHoldForParticipant(contractId, (String) auth.getPrincipal(), isAdmin(auth))));
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
        escrowService.refundDeposit(contractId, hold.getTenantId(), hold.getAmount(), reason, (String) auth.getPrincipal());
        return ResponseEntity.ok(ApiResponse.success("Da hoan tien coc cho nguoi thue",
                escrowService.getHoldForParticipant(contractId, (String) auth.getPrincipal(), true)));
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
    }
}

