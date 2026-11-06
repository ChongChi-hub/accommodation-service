package com.service.admin.controller;

import com.service.admin.dto.CreateReportRequest;
import com.service.admin.dto.UpdateReportStatusRequest;
import com.service.admin.service.ReportService;
import com.service.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createReport(
            Authentication authentication,
            @Valid @RequestBody CreateReportRequest request
    ) {
        String reporterId = (String) authentication.getPrincipal();
        reportService.createReport(reporterId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.message("Gửi báo cáo thành công"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType
    ) {
        List<Map<String, Object>> reports = reportService.listReports(status, targetType);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateReportStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateReportStatusRequest request
    ) {
        reportService.updateReportStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.message("Cập nhật trạng thái báo cáo thành công"));
    }
}

