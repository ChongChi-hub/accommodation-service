package vn.cozynest.billing.interfaces.rest;

import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<DashboardStatsDto.LandlordFinancialStats>> getLandlordStats(Authentication authentication) {
        String landlordId = (String) authentication.getPrincipal();
        DashboardStatsDto.LandlordFinancialStats stats = dashboardService.getLandlordFinancialStats(landlordId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<DashboardStatsDto.AdminFinancialStats>> getAdminStats() {
        DashboardStatsDto.AdminFinancialStats stats = dashboardService.getAdminFinancialStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}

