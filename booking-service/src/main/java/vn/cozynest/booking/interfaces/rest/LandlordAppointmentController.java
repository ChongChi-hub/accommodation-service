package vn.cozynest.booking.interfaces.rest;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.booking.application.dto.*;
import vn.cozynest.booking.application.service.LandlordAppointmentService;
import vn.cozynest.booking.domain.model.AppointmentStatus;
import vn.cozynest.booking.infrastructure.security.UserContextResolver;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.common.dto.PaginationResponse;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/landlord/appointments")
@PreAuthorize("hasRole('LANDLORD')")
public class LandlordAppointmentController {

    private final LandlordAppointmentService landlordAppointmentService;
    private final UserContextResolver userContextResolver;

    public LandlordAppointmentController(
            LandlordAppointmentService landlordAppointmentService,
            UserContextResolver userContextResolver
    ) {
        this.landlordAppointmentService = landlordAppointmentService;
        this.userContextResolver = userContextResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ViewingAppointmentListItemDto>>> getAppointments(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) String roomId,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "scheduledStart") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        String landlordId = userContextResolver.getCurrentUserId();
        AppointmentFilterParams params = new AppointmentFilterParams(
                status, roomId, dateFrom, dateTo, search, page, size, sortBy, sortDirection);
        Page<ViewingAppointmentListItemDto> result = landlordAppointmentService.getAppointments(landlordId, params);
        PaginationResponse pagination = PaginationResponse.of(result.getNumber(), result.getSize(), result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.paginated(result.getContent(), pagination));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AppointmentSummaryDto>> getSummary() {
        String landlordId = userContextResolver.getCurrentUserId();
        AppointmentSummaryDto summary = landlordAppointmentService.getSummary(landlordId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ViewingAppointmentDetailDto>> getDetail(@PathVariable String id) {
        String landlordId = userContextResolver.getCurrentUserId();
        ViewingAppointmentDetailDto detail = landlordAppointmentService.getDetail(landlordId, id);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<ViewingAppointmentDetailDto>> confirmAppointment(
            @PathVariable String id,
            @RequestBody(required = false) ConfirmAppointmentRequest request
    ) {
        String landlordId = userContextResolver.getCurrentUserId();
        ViewingAppointmentDetailDto detail = landlordAppointmentService.confirmAppointment(landlordId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Xac nhan lich hen thanh cong", detail));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ViewingAppointmentDetailDto>> cancelAppointment(
            @PathVariable String id,
            @RequestBody CancelAppointmentRequest request
    ) {
        String landlordId = userContextResolver.getCurrentUserId();
        ViewingAppointmentDetailDto detail = landlordAppointmentService.cancelAppointment(landlordId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Huy lich hen thanh cong", detail));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<ViewingAppointmentDetailDto>> completeAppointment(
            @PathVariable String id,
            @RequestBody CompleteAppointmentRequest request
    ) {
        String landlordId = userContextResolver.getCurrentUserId();
        ViewingAppointmentDetailDto detail = landlordAppointmentService.completeAppointment(landlordId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Cap nhat ket qua lich hen thanh cong", detail));
    }
}
