package vn.cozynest.billing.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.dto.UtilityReadingDto;
import vn.cozynest.billing.application.service.UtilityReadingService;
import vn.cozynest.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/billing/utility-readings")
public class UtilityReadingController {
    private final UtilityReadingService readingService;

    public UtilityReadingController(UtilityReadingService readingService) {
        this.readingService = readingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UtilityReadingDto.ReadingResponse>> recordReading(
            @Valid @RequestBody UtilityReadingDto.RecordReadingCommand command,
            Authentication authentication) {
        String actorId = authentication != null ? (String) authentication.getPrincipal() : "system";
        try {
            UtilityReadingDto.ReadingResponse response = readingService.recordReading(command, actorId);
            return ResponseEntity.ok(ApiResponse.success("Ghi nhận chỉ số điện nước thành công", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "ERR_INVALID_READING"));
        }
    }

    @GetMapping("/contract/{contractId}/latest")
    public ResponseEntity<ApiResponse<UtilityReadingDto.ReadingResponse>> getLatest(@PathVariable String contractId) {
        UtilityReadingDto.ReadingResponse response = readingService.getLatestReading(contractId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

