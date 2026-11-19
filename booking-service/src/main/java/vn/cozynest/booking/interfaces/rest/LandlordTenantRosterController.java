package vn.cozynest.booking.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.cozynest.booking.application.dto.LandlordTenantRosterDto;
import vn.cozynest.booking.application.service.LandlordTenantRosterService;
import vn.cozynest.booking.infrastructure.security.UserContextResolver;
import vn.cozynest.common.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/booking/contracts/landlord")
public class LandlordTenantRosterController {

    private final LandlordTenantRosterService rosterService;
    private final UserContextResolver userContextResolver;

    public LandlordTenantRosterController(
            LandlordTenantRosterService rosterService,
            UserContextResolver userContextResolver
    ) {
        this.rosterService = rosterService;
        this.userContextResolver = userContextResolver;
    }

    @GetMapping("/tenants")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<LandlordTenantRosterDto>>> getActiveTenants() {
        String landlordId = userContextResolver.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(rosterService.getActiveTenantsForLandlord(landlordId)));
    }
}
