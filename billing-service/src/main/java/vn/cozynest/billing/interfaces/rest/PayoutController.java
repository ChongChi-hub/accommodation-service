package vn.cozynest.billing.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.dto.PayoutDto;
import vn.cozynest.billing.application.service.PayoutService;
import vn.cozynest.billing.domain.model.RiskProfile;
import vn.cozynest.common.dto.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing/payouts")
@PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
public class PayoutController {
    private final PayoutService payoutService;

    public PayoutController(PayoutService payoutService) {
        this.payoutService = payoutService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PayoutDto.PayoutResponse>> requestPayout(
            @Valid @RequestBody PayoutDto.RequestPayoutCommand command,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        String landlordId = (String) authentication.getPrincipal();
        PayoutDto.PayoutResponse response = payoutService.requestPayout(landlordId, command, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success("Đã tạo yêu cầu rút tiền và tạm giữ số dư chờ Admin duyệt chuyển khoản", response));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<PayoutDto.PayoutResponse>> cancelPayout(
            @PathVariable String id,
            Authentication authentication) {
        String landlordId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Đã hủy yêu cầu rút tiền và hoàn số dư về ví khả dụng",
                payoutService.cancelPayout(id, landlordId)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PayoutDto.PayoutResponse>> approvePayout(
            @PathVariable String id,
            Authentication authentication) {
        String checkerId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Đã phê duyệt yêu cầu rút tiền",
                payoutService.approvePayout(id, checkerId)));
    }

    @PostMapping("/{id}/confirm-paid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PayoutDto.PayoutResponse>> confirmPayoutPaid(
            @PathVariable String id,
            @Valid @RequestBody PayoutDto.ConfirmPayoutPaidCommand command,
            Authentication authentication) {
        String adminId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận chuyển khoản rút tiền thành công",
                payoutService.confirmPayoutPaid(id, adminId, command)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PayoutDto.PayoutResponse>> rejectPayout(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String checkerId = (String) authentication.getPrincipal();
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối yêu cầu rút tiền và trả về số dư khả dụng",
                payoutService.rejectPayout(id, checkerId, reason)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PayoutDto.PayoutResponse>>> getAdminPayouts() {
        return ResponseEntity.ok(ApiResponse.success(payoutService.getAllPayouts()));
    }

    @PostMapping("/bank-account-changed")
    public ResponseEntity<ApiResponse<RiskProfile>> notifyBankChanged(
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String landlordId = (String) authentication.getPrincipal();
        String otp = body != null ? body.get("otp") : null;
        if (otp == null || otp.trim().isEmpty()) {
            throw new org.springframework.security.access.AccessDeniedException("Yêu cầu mã TOTP để đổi tài khoản ngân hàng");
        }
        try {
            return ResponseEntity.ok(ApiResponse.success("Đã kích hoạt khóa bảo vệ 24 giờ khi đổi tài khoản ngân hàng",
                    payoutService.markBankChangedByOwner(landlordId, otp)));
        } catch (IllegalArgumentException ex) {
            throw new org.springframework.security.access.AccessDeniedException(ex.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PayoutDto.PayoutResponse>>> getPayouts(Authentication authentication) {
        String landlordId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(payoutService.getLandlordPayouts(landlordId)));
    }
}
