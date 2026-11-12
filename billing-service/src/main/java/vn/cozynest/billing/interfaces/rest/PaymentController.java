package vn.cozynest.billing.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.dto.PaymentDto;
import vn.cozynest.billing.application.exception.WebhookSecurityException;
import vn.cozynest.billing.application.service.PaymentReconciliationService;
import vn.cozynest.billing.application.service.PaymentService;
import vn.cozynest.billing.domain.model.PaymentWebhookLog;
import vn.cozynest.billing.domain.model.ReconciliationReport;
import vn.cozynest.billing.domain.repository.PaymentWebhookLogRepository;
import vn.cozynest.common.dto.ApiResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentReconciliationService reconciliationService;
    private final PaymentWebhookLogRepository webhookLogRepository;
    private final Environment environment;

    @Autowired
    public PaymentController(PaymentService paymentService,
                             PaymentReconciliationService reconciliationService,
                             PaymentWebhookLogRepository webhookLogRepository,
                             @Autowired(required = false) Environment environment) {
        this.paymentService = paymentService;
        this.reconciliationService = reconciliationService;
        this.webhookLogRepository = webhookLogRepository;
        this.environment = environment;
    }

    @PostMapping("/intent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentDto.IntentResponse>> createIntent(
            @Valid @RequestBody PaymentDto.CreateIntentRequest request,
            Authentication authentication) {
        String payerId = String.valueOf(authentication.getPrincipal());
        return ResponseEntity.ok(ApiResponse.success("Tạo yêu cầu thanh toán thành công", paymentService.createIntent(payerId, request)));
    }

    @PostMapping("/intent/{paymentCode}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentDto.IntentResponse>> cancelIntent(
            @PathVariable String paymentCode,
            Authentication authentication) {
        String actorId = String.valueOf(authentication.getPrincipal());
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return ResponseEntity.ok(ApiResponse.success("Đã hủy yêu cầu thanh toán", paymentService.cancelIntent(paymentCode, actorId, isAdmin)));
    }

    @GetMapping("/intent/{paymentCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentDto.IntentResponse>> getIntent(
            @PathVariable String paymentCode,
            Authentication authentication) {
        String actorId = String.valueOf(authentication.getPrincipal());
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return ResponseEntity.ok(ApiResponse.success(paymentService.getIntent(paymentCode, actorId, isAdmin)));
    }

    @PostMapping("/sandbox/simulate-webhook")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentDto.IntentResponse>> simulateSandboxWebhook(
            @RequestBody Map<String, Object> payload,
            Authentication authentication) {
        boolean isProd = environment != null && Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));
        if (isProd) {
            return ResponseEntity.notFound().build();
        }
        String paymentCode = String.valueOf(payload.getOrDefault("paymentCode", ""));
        long amount = payload.containsKey("amount") && payload.get("amount") != null
                ? Long.parseLong(String.valueOf(payload.get("amount")))
                : 0L;
        String actorId = String.valueOf(authentication.getPrincipal());
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return ResponseEntity.ok(ApiResponse.success("Mo phong webhook thanh cong (SANDBOX)",
                paymentService.simulateSandboxWebhook(paymentCode, amount, actorId, isAdmin)));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Signature", required = false) String headerSig,
            @RequestHeader(value = "X-Timestamp", required = false) Long headerTimestamp,
            @RequestParam(value = "signature", required = false) String paramSig,
            @RequestParam(value = "timestamp", required = false) Long paramTimestamp,
            HttpServletRequest request) {
        String sig = headerSig != null ? headerSig : paramSig;
        Long timestamp = headerTimestamp != null ? headerTimestamp : paramTimestamp;
        if (sig == null || sig.isBlank() || timestamp == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Thiếu X-Signature hoặc X-Timestamp webhook HMAC", "ERR_INVALID_SIGNATURE"));
        }
        try {
            String sourceIp = request != null ? request.getRemoteAddr() : "127.0.0.1";
            boolean success = paymentService.processWebhook(rawBody, sig, timestamp, sourceIp);
            return ResponseEntity.ok(ApiResponse.success("Xử lý webhook thanh toán thành công", Map.of("success", success)));
        } catch (WebhookSecurityException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(ApiResponse.error(e.getMessage(), "ERR_PAYMENT_SIGNATURE_INVALID"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage(), "ERR_PAYMENT_SIGNATURE_INVALID"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "ERR_PAYMENT_PAYLOAD_INVALID"));
        }
    }

    @GetMapping("/manual-queue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentDto.IntentResponse>>> manualQueue() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getManualInterventionQueue()));
    }

    @PostMapping("/reconcile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentReconciliationService.GatewayReconciliationReport>> reconcilePayments() {
        return ResponseEntity.ok(ApiResponse.success(reconciliationService.reconcileGatewaySettlements()));
    }

    @GetMapping("/reconcile/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ReconciliationReport>>> listReconciliationReports() {
        return ResponseEntity.ok(ApiResponse.success(reconciliationService.getRecentReports()));
    }

    @PostMapping("/reconcile/reports/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReconciliationReport>> resolveReconciliationReport(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String adminId = String.valueOf(authentication.getPrincipal());
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.success(reconciliationService.resolveReport(id, adminId, notes)));
    }

    @GetMapping("/webhooks/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentWebhookLog>>> listWebhookLogs() {
        return ResponseEntity.ok(ApiResponse.success(webhookLogRepository.findRecent()));
    }
}
