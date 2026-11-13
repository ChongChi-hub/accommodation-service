package vn.cozynest.booking.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.booking.application.dto.*;
import vn.cozynest.booking.application.service.ContractService;
import vn.cozynest.booking.infrastructure.security.UserContextResolver;
import vn.cozynest.common.dto.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/booking/contracts", "/api/v1/contracts"})
public class ContractController {

    private final ContractService contractService;
    private final UserContextResolver userContextResolver;

    public ContractController(ContractService contractService, UserContextResolver userContextResolver) {
        this.contractService = contractService;
        this.userContextResolver = userContextResolver;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<ContractDto>> createContract(@Valid @RequestBody CreateContractDto dto) {
        return ResponseEntity.ok(ApiResponse.success(contractService.createContract(dto)));
    }

    @PutMapping("/{id}/draft")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<ContractDto>> updateDraft(
            @PathVariable String id,
            @Valid @RequestBody CreateContractDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(contractService.updateDraftContract(
                id, userContextResolver.getCurrentUserId(), userContextResolver.hasRole("ADMIN"), dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ContractDto>> getContract(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(contractService.getContractById(
                id, userContextResolver.getCurrentUserId(), userContextResolver.hasRole("ADMIN"))));
    }

    @PostMapping("/{id}/sign-tenant")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    public ResponseEntity<ApiResponse<ContractDto>> signTenant(
            @PathVariable String id,
            @Valid @RequestBody SignContractDto dto,
            HttpServletRequest request
    ) {
        String userId = userContextResolver.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(contractService.signContract(
                id, userId, dto.otpCode(), false, extractIp(request), request.getHeader("User-Agent"))));
    }

    @PostMapping("/{id}/sign-landlord")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<ContractDto>> signLandlord(
            @PathVariable String id,
            @Valid @RequestBody SignContractDto dto,
            HttpServletRequest request
    ) {
        String userId = userContextResolver.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(contractService.signContract(
                id, userId, dto.otpCode(), true, extractIp(request), request.getHeader("User-Agent"))));
    }

    @GetMapping("/{id}/pdf/signed-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, String>>> getSignedPdfUrl(@PathVariable String id) {
        String url = contractService.getSignedPdfDownloadUrl(
                id, userContextResolver.getCurrentUserId(), userContextResolver.hasRole("ADMIN"));
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }

    @GetMapping("/{id}/pdf/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadSignedPdf(@PathVariable String id, @RequestParam String token) {
        byte[] bytes = contractService.downloadSignedPdf(
                id, userContextResolver.getCurrentUserId(), userContextResolver.hasRole("ADMIN"), token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contract-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @PostMapping("/{id}/occupants")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ContractOccupantDto>> addOccupant(
            @PathVariable String id,
            @Valid @RequestBody AddOccupantDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(contractService.addOccupant(
                id, userContextResolver.getCurrentUserId(), userContextResolver.hasRole("ADMIN"), dto)));
    }

    @GetMapping("/{id}/occupants")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ContractOccupantDto>>> getOccupants(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(contractService.getOccupants(
                id, userContextResolver.getCurrentUserId(), userContextResolver.hasRole("ADMIN"))));
    }

    @DeleteMapping("/{id}/occupants/{occupantId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> removeOccupant(@PathVariable String id, @PathVariable String occupantId) {
        contractService.removeOccupant(
                id, occupantId, userContextResolver.getCurrentUserId(), userContextResolver.hasRole("ADMIN"));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/amendments")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<ContractAmendmentDto>> createAmendment(
            @PathVariable String id,
            @Valid @RequestBody CreateAmendmentDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(contractService.createAmendment(
                id, userContextResolver.getCurrentUserId(), userContextResolver.hasRole("ADMIN"), dto)));
    }

    @PostMapping("/amendments/{amendmentId}/sign-tenant")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    public ResponseEntity<ApiResponse<ContractAmendmentDto>> signAmendmentTenant(
            @PathVariable String amendmentId,
            @Valid @RequestBody SignContractDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(contractService.signAmendment(
                amendmentId, userContextResolver.getCurrentUserId(), dto.otpCode(), false)));
    }

    @PostMapping("/amendments/{amendmentId}/sign-landlord")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<ContractAmendmentDto>> signAmendmentLandlord(
            @PathVariable String amendmentId,
            @Valid @RequestBody SignContractDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(contractService.signAmendment(
                amendmentId, userContextResolver.getCurrentUserId(), dto.otpCode(), true)));
    }

    @GetMapping("/{id}/amendments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ContractAmendmentDto>>> getAmendments(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(contractService.getAmendments(
                id, userContextResolver.getCurrentUserId(), userContextResolver.hasRole("ADMIN"))));
    }

    @GetMapping("/tenant")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ContractDto>>> getTenantContracts() {
        return ResponseEntity.ok(ApiResponse.success(contractService.getContractsByTenant(userContextResolver.getCurrentUserId())));
    }

    @GetMapping("/landlord")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ContractDto>>> getLandlordContracts() {
        return ResponseEntity.ok(ApiResponse.success(contractService.getContractsByLandlord(userContextResolver.getCurrentUserId())));
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
