package com.service.booking.controller;

import com.service.booking.dto.AcceptRentalRequestDto;
import com.service.booking.dto.CreateRentalRequestDto;
import com.service.booking.service.RentalRequestService;
import com.service.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/rental-requests")
public class RentalRequestController {

    private final RentalRequestService rentalRequestService;

    public RentalRequestController(RentalRequestService rentalRequestService) {
        this.rentalRequestService = rentalRequestService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createRentalRequest(
            Authentication authentication,
            @Valid @RequestBody CreateRentalRequestDto request
    ) {
        String tenantId = (String) authentication.getPrincipal();
        String requestId = rentalRequestService.createRentalRequest(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gửi yêu cầu thuê phòng thành công", Map.of("id", requestId)));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listRentalRequests(
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

        Map<String, Object> result = rentalRequestService.listRentalRequests(userId, role, status, page, limit);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<Map<String, String>>> acceptRequest(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody(required = false) AcceptRentalRequestDto dto
    ) {
        String landlordId = (String) authentication.getPrincipal();
        String contractId = rentalRequestService.acceptRequest(id, landlordId, dto);
        return ResponseEntity.ok(ApiResponse.success("Chấp nhận yêu cầu thuê thành công", Map.of("contractId", contractId)));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectRequest(
            Authentication authentication,
            @PathVariable String id
    ) {
        String landlordId = (String) authentication.getPrincipal();
        rentalRequestService.rejectRequest(id, landlordId);
        return ResponseEntity.ok(ApiResponse.message("Từ chối yêu cầu thành công"));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelRequest(
            Authentication authentication,
            @PathVariable String id
    ) {
        String tenantId = (String) authentication.getPrincipal();
        rentalRequestService.cancelRequest(id, tenantId);
        return ResponseEntity.ok(ApiResponse.message("Hủy yêu cầu thành công"));
    }
}

