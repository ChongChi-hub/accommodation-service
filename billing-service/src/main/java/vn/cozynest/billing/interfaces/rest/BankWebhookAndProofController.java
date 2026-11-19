package vn.cozynest.billing.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.dto.PaymentDto;
import vn.cozynest.billing.application.exception.WebhookSecurityException;
import vn.cozynest.billing.application.port.NotificationProvider;
import vn.cozynest.billing.application.service.AdminManualPaymentApprovalService;
import vn.cozynest.billing.application.service.BankInflowWebhookService;
import vn.cozynest.billing.application.service.PaymentProofStorageService;
import vn.cozynest.billing.domain.model.PaymentIntent;
import vn.cozynest.billing.domain.model.PaymentProofRecord;
import vn.cozynest.billing.domain.model.UnmatchedBankInflow;
import vn.cozynest.common.dto.ApiResponse;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing/payments")
public class BankWebhookAndProofController {
    private final BankInflowWebhookService webhookService;
    private final AdminManualPaymentApprovalService adminApprovalService;
    private final PaymentProofStorageService proofService;
    private final NotificationProvider notificationProvider;
    private final String fintechMode;
    private final String platformAccountNumber;
    private final boolean autoVerifyWebhookEnabled;

    @Autowired
    public BankWebhookAndProofController(
            BankInflowWebhookService webhookService,
            AdminManualPaymentApprovalService adminApprovalService,
            PaymentProofStorageService proofService,
            NotificationProvider notificationProvider,
            @Value("${fintech.mode:SANDBOX_ONLY}") String fintechMode,
            @Value("${fintech.platform.account-number:1042701900}") String platformAccountNumber,
            @Value("${fintech.webhook.auto-verify-enabled:false}") boolean autoVerifyWebhookEnabled) {
        this.webhookService = webhookService;
        this.adminApprovalService = adminApprovalService;
        this.proofService = proofService;
        this.notificationProvider = notificationProvider;
        this.fintechMode = fintechMode;
        this.platformAccountNumber = platformAccountNumber;
        this.autoVerifyWebhookEnabled = autoVerifyWebhookEnabled;
    }

    @PostMapping("/webhook/provider")
    public ResponseEntity<ApiResponse<BankInflowWebhookService.WebhookOutcome>> handleProviderWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Signature", required = false) String headerSig,
            @RequestHeader(value = "X-Timestamp", required = false) Long headerTs,
            @RequestHeader Map<String, String> allHeaders,
            HttpServletRequest request) {
        if (!autoVerifyWebhookEnabled) {
            var disabled = new BankInflowWebhookService.WebhookOutcome(
                    "AUTO_WEBHOOK_DISABLED", false, null, null,
                    "Luồng xác minh webhook tự động đang tắt; Admin kiểm tra tài khoản sàn và duyệt giao dịch thủ công");
            return ResponseEntity.ok(ApiResponse.success(disabled.message(), disabled));
        }
        if (headerSig == null || headerSig.isBlank() || headerTs == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Thiếu X-Signature hoặc X-Timestamp webhook", "ERR_INVALID_SIGNATURE"));
        }
        try {
            String sourceIp = request != null ? request.getRemoteAddr() : "127.0.0.1";
            var outcome = webhookService.processProviderWebhook(rawBody, headerSig, headerTs, sourceIp, allHeaders);
            return ResponseEntity.ok(ApiResponse.success(outcome.message(), outcome));
        } catch (WebhookSecurityException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(ApiResponse.error(e.getMessage(), "ERR_WEBHOOK_SECURITY"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "ERR_WEBHOOK_INVALID"));
        }
    }

    @PostMapping("/sandbox/fake-inflow")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BankInflowWebhookService.WebhookOutcome>> triggerFakeProviderInflow(
            @RequestBody Map<String, Object> body) {
        if (!"SANDBOX_ONLY".equalsIgnoreCase(fintechMode)) return ResponseEntity.notFound().build();
        String txId = String.valueOf(body.getOrDefault("providerTxId", "FT" + System.currentTimeMillis()));
        long amount = body.get("amount") instanceof Number n ? n.longValue() : 0L;
        String content = String.valueOf(body.getOrDefault("content", body.getOrDefault("paymentCode", "")));
        String direction = String.valueOf(body.getOrDefault("direction", "IN"));
        String receiverAcc = String.valueOf(body.getOrDefault("receiverAccount", platformAccountNumber));
        long ts = Instant.now().getEpochSecond();
        String rawBody = String.format(
                "{\"providerTxId\":\"%s\",\"direction\":\"%s\",\"amount\":%d,\"receiverAccount\":\"%s\",\"content\":\"%s\",\"timestamp\":%d}",
                txId, direction, amount, receiverAcc, content.replace("\"", ""), ts);
        String sig = notificationProvider.computeTestSignature(rawBody, ts);
        var outcome = webhookService.processProviderWebhook(rawBody, sig, ts, "127.0.0.1", new HashMap<>());
        return ResponseEntity.ok(ApiResponse.success(outcome.message(), outcome));
    }

    @GetMapping("/admin/pending-approvals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentDto.AdminApprovalItem>>> listPendingAdminApprovals() {
        return ResponseEntity.ok(ApiResponse.success(adminApprovalService.listPendingApprovals()));
    }

    @GetMapping("/unmatched")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UnmatchedBankInflow>>> listUnmatchedInflows(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(webhookService.listUnmatchedInflows(status)));
    }

    @PostMapping("/manual-confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> manualConfirmByAdmin(
            @RequestBody Map<String, Object> body, Authentication auth) {
        String paymentCode = String.valueOf(body.getOrDefault("paymentCode", ""));
        String bankTxCode = String.valueOf(body.getOrDefault("bankTxCode", ""));
        long actualAmount = body.get("actualAmount") instanceof Number n ? n.longValue() : 0L;
        String reason = String.valueOf(body.getOrDefault("reason", ""));
        String checkerAdminId = body.get("checkerAdminId") != null ? String.valueOf(body.get("checkerAdminId")) : null;
        PaymentIntent confirmed = adminApprovalService.approveByAdmin(
                paymentCode, bankTxCode, actualAmount, reason, String.valueOf(auth.getPrincipal()), checkerAdminId);
        return ResponseEntity.ok(ApiResponse.success("Admin đã duyệt giao dịch và kích hoạt ký quỹ Escrow thành công",
                Map.of("paymentCode", confirmed.getPaymentCode(), "status", confirmed.getStatus(), "paidAmount", confirmed.getPaidAmount())));
    }

    @PostMapping("/manual-reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> manualRejectByAdmin(
            @RequestBody Map<String, Object> body, Authentication auth) {
        String paymentCode = String.valueOf(body.getOrDefault("paymentCode", ""));
        String reason = String.valueOf(body.getOrDefault("reason", ""));
        PaymentIntent rejected = adminApprovalService.rejectByAdmin(paymentCode, reason, String.valueOf(auth.getPrincipal()));
        return ResponseEntity.ok(ApiResponse.success("Admin đã hủy giao dịch không hợp lệ thành công",
                Map.of("paymentCode", rejected.getPaymentCode(), "status", rejected.getStatus())));
    }

    @GetMapping("/proofs/{paymentCode}/meta")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentProofRecord>> getProofMeta(@PathVariable String paymentCode, Authentication auth) {
        PaymentProofRecord record = proofService.findAuthorizedMetadata(paymentCode, String.valueOf(auth.getPrincipal()), isAdminOrModerator(auth))
                .orElseThrow(() -> new WebhookSecurityException(404, "Không tìm thấy bằng chứng thanh toán"));
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    @GetMapping("/proofs/{paymentCode}/image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> streamProofImage(@PathVariable String paymentCode, Authentication auth, HttpServletRequest request) {
        String role = auth.getAuthorities().stream().findFirst().map(Object::toString).orElse("UNKNOWN");
        String ip = request != null ? request.getRemoteAddr() : "127.0.0.1";
        PaymentProofStorageService.StreamedProof streamed = proofService.loadAuthorizedProofImage(
                paymentCode, String.valueOf(auth.getPrincipal()), isAdminOrModerator(auth), role, ip);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header("Pragma", "no-cache").header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.IMAGE_JPEG).body(streamed.imageBytes());
    }

    private boolean isAdminOrModerator(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()) || "ROLE_MODERATOR".equalsIgnoreCase(a.getAuthority()));
    }
}
