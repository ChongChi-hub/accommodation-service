package com.service.admin.controller;

import com.service.admin.dto.UpdateUserRoleRequest;
import com.service.admin.service.AdminService;
import com.service.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getStats()));
    }

    @GetMapping("/rooms")
    public ResponseEntity<Map<String, Object>> listAllRooms(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        Map<String, Object> result = adminService.listAllRooms(status, search, page, limit);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> listAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String kycStatus
    ) {
        Map<String, Object> result = adminService.listAllUsers(role, search, kycStatus, page, limit);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<Void>> updateUserRole(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        adminService.updateUserRole(id, request.getRole());
        return ResponseEntity.ok(ApiResponse.message("Cập nhật vai trò người dùng thành công"));
    }

    @GetMapping("/users/{id}/kyc")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserVerification(@PathVariable String id) {
        Map<String, Object> result = adminService.getUserVerification(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/reports/target/{type}/{id}")
    public ResponseEntity<ApiResponse<Object>> getReportTarget(
            @PathVariable String type,
            @PathVariable String id
    ) {
        Object result = adminService.getReportTarget(type, id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

