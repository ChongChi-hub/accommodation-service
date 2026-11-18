package vn.cozynest.billing.interfaces.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.service.EscrowService;
import vn.cozynest.billing.domain.model.EscrowHold;
import vn.cozynest.common.dto.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/internal/billing/escrow")
public class InternalBillingEscrowController {

    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final EscrowService escrowService;
    private final String expectedServiceKey;

    @Autowired
    public InternalBillingEscrowController(
            EscrowService escrowService,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String expectedServiceKey,
            @Autowired(required = false) Environment env
    ) {
        this.escrowService = escrowService;
        this.expectedServiceKey = expectedServiceKey;
        if (env != null && env.matchesProfiles("prod", "production", "staging")) {
            if (DEFAULT_INTERNAL_KEY.equals(expectedServiceKey)) {
                throw new IllegalStateException("Restricted environment requires explicit app.internal.service-key");
            }
        }
    }

    @PostMapping("/init")
    public ResponseEntity<ApiResponse<EscrowHold>> initEscrowHold(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey
    ) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Truy cap noi bo khong hop le", "ERR_UNAUTHORIZED"));
        }
        String contractId = String.valueOf(body.getOrDefault("contractId", ""));
        String tenantId = String.valueOf(body.getOrDefault("tenantId", ""));
        String landlordId = String.valueOf(body.getOrDefault("landlordId", ""));
        long amount = body.get("amount") instanceof Number n ? n.longValue() : 0L;
        EscrowHold hold = escrowService.openHold(contractId, tenantId, landlordId, amount, 72);
        return ResponseEntity.ok(ApiResponse.success("Khoi tao khoan ky quy thanh cong", hold));
    }

    @PostMapping("/{contractId}/compensate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> compensateEscrow(
            @PathVariable String contractId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey
    ) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Truy cap noi bo khong hop le", "ERR_UNAUTHORIZED"));
        }
        String reason = body != null ? body.get("reason") : "Saga compensation";
        escrowService.compensateHold(contractId, reason, "SYSTEM_SAGA");
        return ResponseEntity.ok(ApiResponse.success("Da xu ly boi hoan/huy ky quy saga", Map.of("compensated", true)));
    }

    @PostMapping("/{contractId}/freeze")
    public ResponseEntity<ApiResponse<EscrowHold>> freezeEscrow(
            @PathVariable String contractId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey
    ) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Truy cap noi bo khong hop le", "ERR_UNAUTHORIZED"));
        }
        String actorId = body != null ? body.getOrDefault("actorId", "SYSTEM_ADMIN") : "SYSTEM_ADMIN";
        String reason = body != null ? body.getOrDefault("reason", "Dong bang tranh chap") : "Dong bang tranh chap";
        return ResponseEntity.ok(ApiResponse.success(escrowService.freezeForDispute(contractId, actorId, true, reason)));
    }

    @PostMapping("/{contractId}/release")
    public ResponseEntity<ApiResponse<EscrowHold>> releaseEscrow(
            @PathVariable String contractId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey
    ) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Truy cap noi bo khong hop le", "ERR_UNAUTHORIZED"));
        }
        String actorId = body != null ? body.getOrDefault("actorId", "SYSTEM_ADMIN") : "SYSTEM_ADMIN";
        EscrowHold hold = escrowService.getHoldForParticipant(contractId, actorId, true);
        if ("DISPUTE_FROZEN".equals(hold.getStatus())) {
            escrowService.unfreezeDispute(contractId);
        }
        escrowService.disburseDeposit(contractId, hold.getLandlordId(), hold.getAmount(), actorId);
        return ResponseEntity.ok(ApiResponse.success(escrowService.getHoldForParticipant(contractId, actorId, true)));
    }

    @PostMapping("/{contractId}/refund")
    public ResponseEntity<ApiResponse<EscrowHold>> refundEscrow(
            @PathVariable String contractId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey
    ) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Truy cap noi bo khong hop le", "ERR_UNAUTHORIZED"));
        }
        String actorId = body != null ? body.getOrDefault("actorId", "SYSTEM_ADMIN") : "SYSTEM_ADMIN";
        String reason = body != null ? body.getOrDefault("reason", "Hoan coc tranh chap") : "Hoan coc tranh chap";
        EscrowHold hold = escrowService.getHoldForParticipant(contractId, actorId, true);
        escrowService.refundDeposit(contractId, hold.getTenantId(), hold.getAmount(), reason, actorId);
        return ResponseEntity.ok(ApiResponse.success(escrowService.getHoldForParticipant(contractId, actorId, true)));
    }

    private boolean isKeyValid(String serviceKey) {
        if (expectedServiceKey == null) return false;
        return MessageDigest.isEqual(
                serviceKey.getBytes(StandardCharsets.UTF_8),
                expectedServiceKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
