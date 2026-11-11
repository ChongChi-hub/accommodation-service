package vn.cozynest.booking.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    public RentalRequestController(RentalRequestService rentalRequestService, UserContextResolver userContextResolver) {
        this.rentalRequestService = rentalRequestService;
        this.userContextResolver = userContextResolver;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    public ResponseEntity<ApiResponse<RentalRequestDto>> createRentalRequest(
            @Valid @RequestBody CreateRentalRequestDto dto
    ) {
        String tenantId = userContextResolver.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(rentalRequestService.createRequest(tenantId, dto)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<RentalRequestDto>> approveRentalRequest(
            @PathVariable String id,
            @RequestParam(required = false) String contractId
    ) {
        String landlordId = userContextResolver.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(rentalRequestService.approveRequest(landlordId, id, contractId)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<RentalRequestDto>> rejectRentalRequest(@PathVariable String id) {
        String landlordId = userContextResolver.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(rentalRequestService.rejectRequest(landlordId, id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RentalRequestDto>> cancelRentalRequest(@PathVariable String id) {
        String actorId = userContextResolver.getCurrentUserId();
        boolean isAdmin = userContextResolver.hasRole("ADMIN");
        return ResponseEntity.ok(ApiResponse.success(rentalRequestService.cancelRequest(actorId, isAdmin, id)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RentalRequestDto>> getRentalRequest(@PathVariable String id) {
        String actorId = userContextResolver.getCurrentUserId();
        boolean isAdmin = userContextResolver.hasRole("ADMIN");
        return ResponseEntity.ok(ApiResponse.success(rentalRequestService.getRequestById(id, actorId, isAdmin)));
    }

    @GetMapping("/tenant")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<RentalRequestDto>>> getTenantRequests() {
        String tenantId = userContextResolver.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(rentalRequestService.getTenantRequests(tenantId)));
    }

    @GetMapping("/landlord")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<RentalRequestDto>>> getLandlordRequests() {
        String landlordId = userContextResolver.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(rentalRequestService.getLandlordRequests(landlordId)));
    }
}
