package vn.cozynest.booking.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.booking.application.dto.CancelAppointmentRequest;
import vn.cozynest.booking.application.dto.CreateAppointmentRequest;
import vn.cozynest.booking.application.dto.ViewingAppointmentListItemDto;
import vn.cozynest.booking.application.service.TenantAppointmentService;
import vn.cozynest.booking.infrastructure.security.UserContextResolver;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.common.dto.PaginationResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenant/appointments")
@PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
public class TenantAppointmentController {

    private final TenantAppointmentService tenantAppointmentService;
    private final UserContextResolver userContextResolver;

    public TenantAppointmentController(
            TenantAppointmentService tenantAppointmentService,
            UserContextResolver userContextResolver
    ) {
        this.tenantAppointmentService = tenantAppointmentService;
        this.userContextResolver = userContextResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ViewingAppointmentListItemDto>>> getTenantAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String tenantId = userContextResolver.getCurrentUserId();
        Page<ViewingAppointmentListItemDto> result = tenantAppointmentService.getTenantAppointments(tenantId, page, size);
        PaginationResponse pagination = PaginationResponse.of(result.getNumber(), result.getSize(), result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.paginated(result.getContent(), pagination));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ViewingAppointmentListItemDto>> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request
    ) {
        String tenantId = userContextResolver.getCurrentUserId();
        ViewingAppointmentListItemDto created = tenantAppointmentService.createAppointment(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success("Dat lich xem phong thanh cong", created));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ViewingAppointmentListItemDto>> cancelAppointment(
            @PathVariable String id,
            @RequestBody(required = false) CancelAppointmentRequest request
    ) {
        String tenantId = userContextResolver.getCurrentUserId();
        ViewingAppointmentListItemDto cancelled = tenantAppointmentService.cancelAppointment(tenantId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Huy lich hen thanh cong", cancelled));
    }
}
