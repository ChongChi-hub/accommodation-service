package vn.cozynest.billing.interfaces.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.cozynest.billing.application.dto.DashboardStatsDto;
import vn.cozynest.billing.application.service.BillingDashboardService;
import vn.cozynest.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/billing/dashboard")
public class BillingDashboardController {
    private final BillingDashboardService dashboardService;

    public BillingDashboardController(BillingDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/landlord")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsDto.LandlordFinancialStats>> getLandlordStats(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getLandlordFinancialStats((String) auth.getPrincipal())));
    }

    @GetMapping("/tenant")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsDto.TenantFinancialStats>> getTenantStats(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getTenantFinancialStats((String) auth.getPrincipal())));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsDto.AdminFinancialStats>> getAdminStats() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getAdminFinancialStats()));
    }

    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportStatsCsv(Authentication auth) {
        boolean isLandlord = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_LANDLORD".equalsIgnoreCase(a.getAuthority()));
        String scope = isLandlord ? "LANDLORD" : "TENANT";
        byte[] csv = dashboardService.exportInvoicesCsvWithBom(scope, (String) auth.getPrincipal());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"financial-stats.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
