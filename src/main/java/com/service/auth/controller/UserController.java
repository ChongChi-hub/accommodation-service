package com.service.auth.controller;

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

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
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

