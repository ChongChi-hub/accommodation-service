package vn.cozynest.booking.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.booking.application.dto.CreateRentalRequestDto;
import vn.cozynest.booking.application.dto.RentalRequestDto;
import vn.cozynest.booking.application.service.RentalRequestService;
import vn.cozynest.booking.infrastructure.security.UserContextResolver;
import vn.cozynest.common.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/booking/rental-requests")
public class RentalRequestController {

    private final RentalRequestService rentalRequestService;
    private final UserContextResolver userContextResolver;

    public RentalRequestController(RentalRequestService rentalRequestService,
                                   UserContextResolver userContextResolver) {
        this.rentalRequestService = rentalRequestService;
        this.userContextResolver = userContextResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RentalRequestDto>> createRentalRequest(
            @Valid @RequestBody CreateRentalRequestDto dto) {
        String tenantId = userContextResolver.getCurrentUserId();
        RentalRequestDto result = rentalRequestService.createRequest(tenantId, dto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<RentalRequestDto>> approveRentalRequest(
            @PathVariable String id,
            @RequestParam(required = false) String contractId) {
        String landlordId = userContextResolver.getCurrentUserId();
        RentalRequestDto result = rentalRequestService.approveRequest(landlordId, id, contractId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<RentalRequestDto>> rejectRentalRequest(@PathVariable String id) {
        String landlordId = userContextResolver.getCurrentUserId();
        RentalRequestDto result = rentalRequestService.rejectRequest(landlordId, id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<RentalRequestDto>> cancelRentalRequest(@PathVariable String id) {
        String tenantId = userContextResolver.getCurrentUserId();
        RentalRequestDto result = rentalRequestService.cancelRequest(tenantId, id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RentalRequestDto>> getRentalRequest(@PathVariable String id) {
        RentalRequestDto result = rentalRequestService.getRequestById(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/tenant")
    public ResponseEntity<ApiResponse<List<RentalRequestDto>>> getTenantRequests() {
        String tenantId = userContextResolver.getCurrentUserId();
        List<RentalRequestDto> results = rentalRequestService.getTenantRequests(tenantId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/landlord")
    public ResponseEntity<ApiResponse<List<RentalRequestDto>>> getLandlordRequests() {
        String landlordId = userContextResolver.getCurrentUserId();
        List<RentalRequestDto> results = rentalRequestService.getLandlordRequests(landlordId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}

