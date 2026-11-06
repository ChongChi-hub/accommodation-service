package com.service.booking.controller;

import com.service.booking.service.ContractService;
import com.service.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listContracts(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status
    ) {
        String userId = (String) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "").toLowerCase())
                .orElse("tenant");

        Map<String, Object> result = contractService.listContracts(userId, role, status, page, limit);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getContractById(
            Authentication authentication,
            @PathVariable String id
    ) {
        String userId = (String) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "").toLowerCase())
                .orElse("tenant");

        Map<String, Object> contract = contractService.getContractById(id, userId, role);
        return ResponseEntity.ok(ApiResponse.success(contract));
    }

    @PatchMapping("/{id}/sign")
    public ResponseEntity<ApiResponse<Void>> signContract(
            Authentication authentication,
            @PathVariable String id
    ) {
        String userId = (String) authentication.getPrincipal();
        contractService.signContract(id, userId);
        return ResponseEntity.ok(ApiResponse.message("Ký hợp đồng thành công"));
    }

    @PatchMapping("/{id}/terminate")
    public ResponseEntity<ApiResponse<Void>> terminateContract(
            Authentication authentication,
            @PathVariable String id
    ) {
        String userId = (String) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "").toLowerCase())
                .orElse("tenant");

        contractService.terminateContract(id, userId, role);
        return ResponseEntity.ok(ApiResponse.message("Chấm dứt hợp đồng thành công"));
    }
}

