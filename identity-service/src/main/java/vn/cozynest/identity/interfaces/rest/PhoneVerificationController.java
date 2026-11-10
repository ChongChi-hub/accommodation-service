package vn.cozynest.identity.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.SendPhoneOtpRequest;
import vn.cozynest.identity.application.dto.VerifyPhoneOtpRequest;
import vn.cozynest.identity.application.service.PhoneVerificationService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/phone")
public class PhoneVerificationController {
    private final PhoneVerificationService phoneVerificationService;

    public PhoneVerificationController(PhoneVerificationService phoneVerificationService) {
        this.phoneVerificationService = phoneVerificationService;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendOtp(@Valid @RequestBody SendPhoneOtpRequest request) {
        phoneVerificationService.sendOtp(request.phoneNumber());
        return ResponseEntity.ok(ApiResponse.success("Mã OTP đã được gửi tới số điện thoại", Map.of("phone", request.phoneNumber())));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(
            @Valid @RequestBody VerifyPhoneOtpRequest request,
            Authentication authentication) {
        String userId = authentication != null ? (String) authentication.getPrincipal() : null;
        boolean verified = phoneVerificationService.verifyOtp(request.phoneNumber(), request.otp(), userId);
        if (!verified) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Mã OTP không chính xác hoặc đã hết hạn", "ERR_AUTH_INVALID_OTP"));
        }
        return ResponseEntity.ok(ApiResponse.success("Xác thực số điện thoại thành công", Map.of("verified", true, "phone", request.phoneNumber())));
    }
}
