package com.service.auth.controller;

import com.service.admin.service.AdminService;
import com.service.auth.dto.KycReviewRequest;
import com.service.auth.dto.KycSubmitRequest;
import com.service.auth.dto.UserDto;
import com.service.auth.entity.UserVerificationEntity;
import com.service.auth.service.AuthService;
import com.service.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;
    private final AdminService adminService;

    public UserController(AuthService authService, AdminService adminService) {
        this.authService = authService;
        this.adminService = adminService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String kycStatus
    ) {
        Map<String, Object> result = adminService.listAllUsers(role, search, kycStatus, page, limit);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable String id) {
        UserDto userDto = authService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(userDto));
    }

    @PostMapping("/kyc")
    public ResponseEntity<ApiResponse<Void>> submitKyc(Authentication authentication,
                                                       @Valid @RequestBody KycSubmitRequest request) {
        String userId = (String) authentication.getPrincipal();
        authService.submitKyc(userId, request);
        return ResponseEntity.ok(ApiResponse.message("Hồ sơ xác thực danh tính đã được gửi"));
    }

    @GetMapping("/kyc/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserVerificationEntity>>> listPendingKyc() {
        List<UserVerificationEntity> pending = authService.listPendingKyc();
        return ResponseEntity.ok(ApiResponse.success(pending));
    }

    @PatchMapping("/kyc/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reviewKyc(Authentication authentication,
                                                       @PathVariable String id,
                                                       @Valid @RequestBody KycReviewRequest request) {
        String reviewerId = (String) authentication.getPrincipal();
        authService.reviewKyc(id, reviewerId, request);
        return ResponseEntity.ok(ApiResponse.message("Đã cập nhật trạng thái hồ sơ xác thực"));
    }
}

