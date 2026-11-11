package vn.cozynest.billing.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.dto.UtilityReadingDto;
import vn.cozynest.billing.application.service.UtilityReadingService;
import vn.cozynest.billing.domain.model.UtilityReadingAudit;
import vn.cozynest.common.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing/utility-readings")
public class UtilityReadingController {
    private final UtilityReadingService readingService;

    public UtilityReadingController(UtilityReadingService readingService) {
        this.readingService = readingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<UtilityReadingDto.ReadingResponse>> recordReading(
            @Valid @RequestBody UtilityReadingDto.RecordReadingCommand command,
            Authentication authentication) {
        String actorId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận chỉ số điện nước thành công",
                readingService.recordReading(command, actorId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<UtilityReadingDto.ReadingResponse>> editReading(
            @PathVariable String id,
            @Valid @RequestBody UtilityReadingDto.EditReadingCommand command,
            Authentication authentication) {
        String actorId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Cập nhật chỉ số điện nước thành công",
                readingService.editReading(id, command, actorId)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<UtilityReadingDto.ReadingResponse>> cancelReading(
            @PathVariable String id,
            @Valid @RequestBody UtilityReadingDto.CancelReadingCommand command,
            Authentication authentication) {
        String actorId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Đã hủy chỉ số điện nước",
                readingService.cancelReading(id, command.reason(), actorId)));
    }

    @GetMapping("/contract/{contractId}/latest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UtilityReadingDto.ReadingResponse>> getLatest(@PathVariable String contractId) {
        return ResponseEntity.ok(ApiResponse.success(readingService.getLatestReading(contractId)));
    }

    @GetMapping("/contract/{contractId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UtilityReadingDto.ReadingResponse>>> getByContract(@PathVariable String contractId) {
        return ResponseEntity.ok(ApiResponse.success(readingService.getReadingsForContract(contractId)));
    }

    @GetMapping("/{id}/audits")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UtilityReadingAudit>>> getAudits(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(readingService.getAuditHistory(id)));
    }
}
