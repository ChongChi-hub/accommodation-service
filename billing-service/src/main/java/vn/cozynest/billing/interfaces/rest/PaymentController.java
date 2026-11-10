package vn.cozynest.billing.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.dto.PaymentDto;
import vn.cozynest.billing.application.service.PaymentService;
import vn.cozynest.common.dto.ApiResponse;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/intent")
    public ResponseEntity<ApiResponse<PaymentDto.IntentResponse>> createIntent(
            @Valid @RequestBody PaymentDto.CreateIntentRequest request,
            Authentication authentication) {
        String payerId = (String) authentication.getPrincipal();
        PaymentDto.IntentResponse response = paymentService.createIntent(payerId, request);
        return ResponseEntity.ok(ApiResponse.success("Tạo yêu cầu thanh toán thành công", response));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Signature", required = false) String headerSig,
            @RequestHeader(value = "X-Timestamp", required = false) Long headerTimestamp,
            @RequestParam(value = "signature", required = false) String paramSig,
            @RequestParam(value = "timestamp", required = false) Long paramTimestamp) {

        String sig = headerSig != null ? headerSig : paramSig;
        long timestamp = headerTimestamp != null ? headerTimestamp : (paramTimestamp != null ? paramTimestamp : Instant.now().getEpochSecond());

        if (sig == null || sig.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Thiếu chữ ký bảo mật webhook HMAC", "ERR_INVALID_SIGNATURE"));
        }

        try {
            boolean success = paymentService.processWebhook(rawBody, sig, timestamp);
            return ResponseEntity.ok(ApiResponse.success("Xử lý webhook thanh toán thành công", Map.of("success", success)));
        } catch (SecurityException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "ERR_PAYMENT_SIGNATURE_INVALID"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "ERR_PAYMENT_PAYLOAD_INVALID"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi xử lý webhook: " + e.getMessage(), "ERR_INTERNAL_ERROR"));
        }
    }
}

