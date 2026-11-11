package vn.cozynest.identity.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.InternalKycStatusResponse;
import vn.cozynest.identity.application.dto.InternalPhoneStatusResponse;
import vn.cozynest.identity.application.dto.InternalVerifyOtpRequest;
import vn.cozynest.identity.application.service.InternalUserService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final InternalUserService internalUserService;
    private final String expectedServiceKey;

    public InternalUserController(
            InternalUserService internalUserService,
            @Value("${app.internal.service-key:CozyNestInternalServiceKeySecret2026}") String expectedServiceKey,
            @Autowired(required = false) Environment env) {
        this.internalUserService = internalUserService;
        this.expectedServiceKey = expectedServiceKey;

        if (env != null && env.matchesProfiles("prod", "production")) {
            String envKey = System.getenv("INTERNAL_SERVICE_KEY");
            if (envKey == null || envKey.isBlank() || "CozyNestInternalServiceKeySecret2026".equals(expectedServiceKey)) {
                throw new IllegalStateException("Production environment must explicitly configure INTERNAL_SERVICE_KEY without default dev key");
            }
        }
    }

    @GetMapping("/{id}/kyc-status")
    public ResponseEntity<ApiResponse<InternalKycStatusResponse>> getKycStatus(
            @PathVariable("id") String userId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey) {

        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Truy cap bi tu choi: Khoa xac thuc dich vu noi bo khong hop le", "ERR_UNAUTHORIZED"));
        }

        InternalKycStatusResponse response = internalUserService.getKycStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/phone-status")
    public ResponseEntity<ApiResponse<InternalPhoneStatusResponse>> getPhoneStatus(
            @PathVariable("id") String userId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Truy cap bi tu choi: Khoa xac thuc dich vu noi bo khong hop le", "ERR_UNAUTHORIZED"));
        }
        return ResponseEntity.ok(ApiResponse.success(internalUserService.getPhoneStatus(userId)));
    }

    @PostMapping("/{id}/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verifyOtp(
            @PathVariable("id") String userId,
            @Valid @RequestBody InternalVerifyOtpRequest request,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Truy cap bi tu choi: Khoa xac thuc dich vu noi bo khong hop le", "ERR_UNAUTHORIZED"));
        }
        boolean verified = internalUserService.verifyPhoneOtpForUser(userId, request.otpCode());
        return ResponseEntity.ok(ApiResponse.success(Map.of("verified", verified)));
    }

    private boolean isKeyValid(String serviceKey) {
        if (expectedServiceKey == null) return false;
        byte[] a = serviceKey.getBytes(StandardCharsets.UTF_8);
        byte[] b = expectedServiceKey.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}

