package com.service.booking.controller;

import com.service.booking.dto.CreateAppointmentRequest;
import com.service.booking.service.AppointmentService;
import com.service.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createAppointment(
            Authentication authentication,
            @Valid @RequestBody CreateAppointmentRequest request
    ) {
        String tenantId = (String) authentication.getPrincipal();
        String appointmentId = appointmentService.createAppointment(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đặt lịch hẹn xem phòng thành công! Email xác nhận đã được gửi.", Map.of("id", appointmentId)));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listAppointments(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status
    ) {
        String userId = (String) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "").toLowerCase())
                .orElse("tenant");

        Map<String, Object> result = appointmentService.listAppointments(userId, role, status, page, limit);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmAppointment(
            Authentication authentication,
            @PathVariable String id
    ) {
        String landlordId = (String) authentication.getPrincipal();
        appointmentService.confirmAppointment(id, landlordId);
        return ResponseEntity.ok(ApiResponse.message("Xác nhận lịch hẹn thành công"));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelAppointment(
            Authentication authentication,
            @PathVariable String id
    ) {
        String userId = (String) authentication.getPrincipal();
        appointmentService.cancelAppointment(id, userId);
        return ResponseEntity.ok(ApiResponse.message("Hủy lịch hẹn thành công"));
    }
}

