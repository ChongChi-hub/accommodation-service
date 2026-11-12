package vn.cozynest.billing.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.dto.PayoutDto;
import vn.cozynest.billing.application.service.PayoutService;
import vn.cozynest.common.dto.ApiResponse;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<ApiResponse<List<PayoutDto.PayoutResponse>>> getPayouts(Authentication authentication) {
        String landlordId = (String) authentication.getPrincipal();
        List<PayoutDto.PayoutResponse> list = payoutService.getLandlordPayouts(landlordId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}

