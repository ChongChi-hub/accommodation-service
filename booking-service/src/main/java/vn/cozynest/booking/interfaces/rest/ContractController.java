package vn.cozynest.booking.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.booking.application.dto.*;
import vn.cozynest.booking.application.service.ContractService;
import vn.cozynest.booking.infrastructure.security.UserContextResolver;
import vn.cozynest.common.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/booking/contracts")
public class ContractController {

    private final ContractService contractService;
    private final UserContextResolver userContextResolver;

    public ContractController(ContractService contractService, UserContextResolver userContextResolver) {
        this.contractService = contractService;
        this.userContextResolver = userContextResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContractDto>> createContract(@Valid @RequestBody CreateContractDto dto) {
        ContractDto contract = contractService.createContract(dto);
        return ResponseEntity.ok(ApiResponse.success(contract));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractDto>> getContract(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(contractService.getContractById(id)));
    }

    @PostMapping("/{id}/sign-tenant")
    public ResponseEntity<ApiResponse<ContractDto>> signTenant(
            @PathVariable String id, @Valid @RequestBody SignContractDto dto) {
        String userId = userContextResolver.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(contractService.signContract(id, userId, dto.otpCode(), false)));
    }

    @PostMapping("/{id}/sign-landlord")
    public ResponseEntity<ApiResponse<ContractDto>> signLandlord(
            @PathVariable String id, @Valid @RequestBody SignContractDto dto) {
        String userId = userContextResolver.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(contractService.signContract(id, userId, dto.otpCode(), true)));
    }

    @PostMapping("/{id}/occupants")
    public ResponseEntity<ApiResponse<ContractOccupantDto>> addOccupant(
            @PathVariable String id, @Valid @RequestBody AddOccupantDto dto) {
        return ResponseEntity.ok(ApiResponse.success(contractService.addOccupant(id, dto)));
    }

    @GetMapping("/{id}/occupants")
    public ResponseEntity<ApiResponse<List<ContractOccupantDto>>> getOccupants(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(contractService.getOccupants(id)));
    }

    @DeleteMapping("/{id}/occupants/{occupantId}")
    public ResponseEntity<ApiResponse<Void>> removeOccupant(
            @PathVariable String id, @PathVariable String occupantId) {
        contractService.removeOccupant(id, occupantId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/amendments")
    public ResponseEntity<ApiResponse<ContractAmendmentDto>> createAmendment(
            @PathVariable String id, @Valid @RequestBody CreateAmendmentDto dto) {
        return ResponseEntity.ok(ApiResponse.success(contractService.createAmendment(id, dto)));
    }

    @PostMapping("/amendments/{amendmentId}/sign-tenant")
    public ResponseEntity<ApiResponse<ContractAmendmentDto>> signAmendmentTenant(@PathVariable String amendmentId) {
        String userId = userContextResolver.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(contractService.signAmendment(amendmentId, userId, false)));
    }

    @PostMapping("/amendments/{amendmentId}/sign-landlord")
    public ResponseEntity<ApiResponse<ContractAmendmentDto>> signAmendmentLandlord(@PathVariable String amendmentId) {
        String userId = userContextResolver.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(contractService.signAmendment(amendmentId, userId, true)));
    }

    @GetMapping("/{id}/amendments")
    public ResponseEntity<ApiResponse<List<ContractAmendmentDto>>> getAmendments(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(contractService.getAmendments(id)));
    }

    @GetMapping("/tenant")
    public ResponseEntity<ApiResponse<List<ContractDto>>> getTenantContracts() {
        String tenantId = userContextResolver.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(contractService.getTenantContracts(tenantId)));
    }

    @GetMapping("/landlord")
    public ResponseEntity<ApiResponse<List<ContractDto>>> getLandlordContracts() {
        String landlordId = userContextResolver.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(contractService.getLandlordContracts(landlordId)));
    }
}

