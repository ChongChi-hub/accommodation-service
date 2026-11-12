package vn.cozynest.identity.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.ChangePhoneRequest;
import vn.cozynest.identity.application.dto.SendPhoneOtpRequest;
import vn.cozynest.identity.application.dto.VerifyPhoneOtpRequest;
import vn.cozynest.identity.application.service.PhoneVerificationService;
import vn.cozynest.identity.domain.service.PhoneNumberPolicy;

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
        String maskedPhone = phoneVerificationService.sendOtp(request.phoneNumber());
        return ResponseEntity.ok(ApiResponse.success("Mã OTP đã được gửi tới số điện thoại", Map.of("phone", maskedPhone)));
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
        String masked = PhoneNumberPolicy.mask(PhoneNumberPolicy.normalizeAndValidate(request.phoneNumber()));
        return ResponseEntity.ok(ApiResponse.success("Xác thực số điện thoại thành công", Map.of("verified", true, "phone", masked)));
    }

    @PostMapping("/change")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changePhone(
            @Valid @RequestBody ChangePhoneRequest request,
            Authentication authentication) {
        String userId = authentication != null ? (String) authentication.getPrincipal() : null;
        String maskedNew = phoneVerificationService.changePhoneNumber(
                userId, request.newPhoneNumber(), request.newOtp(), request.oldOtp());
        return ResponseEntity.ok(ApiResponse.success("Đổi số điện thoại thành công", Map.of("verified", true, "phone", maskedNew)));
    }
}
