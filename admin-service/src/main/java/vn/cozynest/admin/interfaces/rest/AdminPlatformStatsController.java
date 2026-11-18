package vn.cozynest.admin.interfaces.rest;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.cozynest.admin.application.service.AdminPlatformStatsService;
import vn.cozynest.common.dto.ApiResponse;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/stats")
public class AdminPlatformStatsController {

    private final AdminPlatformStatsService platformStatsService;

    public AdminPlatformStatsController(AdminPlatformStatsService platformStatsService) {
        this.platformStatsService = platformStatsService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(ApiResponse.success(platformStatsService.getOverview(from, to)));
    }

    @GetMapping("/timeseries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTimeseries(
            @RequestParam(defaultValue = "appointments") String metric,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String granularity
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                platformStatsService.getTimeseries(metric, from, to, granularity)
        ));
    }
}
