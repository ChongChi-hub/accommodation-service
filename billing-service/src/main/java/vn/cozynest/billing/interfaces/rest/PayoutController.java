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
            Authentication authentication) {
        String landlordId = (String) authentication.getPrincipal();
        PayoutDto.PayoutResponse response = payoutService.requestPayout(landlordId, command);
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu rút tiền đã được thực hiện", response));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PayoutDto.PayoutResponse>> approvePayout(
            @PathVariable String id,
            Authentication authentication) {
        String checkerId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Da phe duyet yeu cau rut tien",
                payoutService.approvePayout(id, checkerId)));
    }

    @PostMapping("/bank-account-changed")
    public ResponseEntity<ApiResponse<RiskProfile>> notifyBankChanged(
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String landlordId = (String) authentication.getPrincipal();
        String otp = body != null ? body.get("otp") : null;
        return ResponseEntity.ok(ApiResponse.success("Da kich hoat khoa bao ve 24 gio khi doi tai khoan ngan hang",
                payoutService.markBankChangedByOwner(landlordId, otp)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PayoutDto.PayoutResponse>>> getPayouts(Authentication authentication) {
        String landlordId = (String) authentication.getPrincipal();
        List<PayoutDto.PayoutResponse> list = payoutService.getLandlordPayouts(landlordId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
