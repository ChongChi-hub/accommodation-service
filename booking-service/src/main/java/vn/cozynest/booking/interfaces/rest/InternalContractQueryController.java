package vn.cozynest.booking.interfaces.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.booking.application.service.ContractService;
import vn.cozynest.common.dto.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;

@RestController
@RequestMapping("/internal/contracts")
public class InternalContractQueryController {

    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final ContractService contractService;
    private final String expectedServiceKey;

    public InternalContractQueryController(
            ContractService contractService,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String expectedServiceKey,
            @Autowired(required = false) Environment env
    ) {
        this.contractService = contractService;
        this.expectedServiceKey = expectedServiceKey;
        if (env != null && env.matchesProfiles("prod", "production", "staging")) {
            if (DEFAULT_INTERNAL_KEY.equals(expectedServiceKey)) {
                throw new IllegalStateException("Restricted environment requires explicit app.internal.service-key");
            }
        }
    }

    @GetMapping("/{contractId}/effective-rent")
    public ResponseEntity<ApiResponse<ContractService.EffectiveRentSnapshot>> getEffectiveRent(
            @PathVariable String contractId,
            @RequestParam(required = false) LocalDate billingDate,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey
    ) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Truy cap noi bo khong hop le", "ERR_UNAUTHORIZED"));
        }
        return ResponseEntity.ok(ApiResponse.success(contractService.getEffectiveRent(contractId, billingDate)));
    }

    private boolean isKeyValid(String serviceKey) {
        if (expectedServiceKey == null) return false;
        return MessageDigest.isEqual(
                serviceKey.getBytes(StandardCharsets.UTF_8),
                expectedServiceKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}

