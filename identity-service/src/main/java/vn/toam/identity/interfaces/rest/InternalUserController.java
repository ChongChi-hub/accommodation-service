package vn.toam.identity.interfaces.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.toam.common.dto.ApiResponse;
import vn.toam.identity.application.dto.InternalKycStatusResponse;
import vn.toam.identity.application.service.InternalUserService;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final InternalUserService internalUserService;
    private final String expectedServiceKey;

    public InternalUserController(
            InternalUserService internalUserService,
            @Value("${app.internal.service-key:ToAmInternalServiceKeySecret2026}") String expectedServiceKey) {
        this.internalUserService = internalUserService;
        this.expectedServiceKey = expectedServiceKey;
    }

    @GetMapping("/{id}/kyc-status")
    public ResponseEntity<ApiResponse<InternalKycStatusResponse>> getKycStatus(
            @PathVariable("id") String userId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey) {

        if (serviceKey == null || !serviceKey.equals(expectedServiceKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Truy cap bi tu choi: Khoa xac thuc dich vu noi bo khong hop le", "ERR_FORBIDDEN"));
        }

        InternalKycStatusResponse response = internalUserService.getKycStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

