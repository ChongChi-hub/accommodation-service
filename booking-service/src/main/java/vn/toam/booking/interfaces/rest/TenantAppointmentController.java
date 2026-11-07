package vn.toam.booking.interfaces.rest;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.toam.booking.application.dto.ViewingAppointmentListItemDto;
import vn.toam.booking.application.service.TenantAppointmentService;
import vn.toam.booking.infrastructure.security.UserContextResolver;
import vn.toam.common.dto.ApiResponse;
import vn.toam.common.dto.PaginationResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenant/appointments")
@PreAuthorize("hasRole('TENANT')")
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
}
