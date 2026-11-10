package vn.cozynest.billing.interfaces.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.service.LedgerService;
import vn.cozynest.common.dto.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing/ledger")
public class LedgerController {
    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/wallet")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWalletBalance(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        String walletAcc = ledgerService.getWalletAccountNumber(userId);
        long balance = ledgerService.getAccountBalance(walletAcc);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "userId", userId,
                "accountNumber", walletAcc,
                "balance", balance,
                "currency", "VND"
        )));
    }

    @GetMapping("/integrity")
    public ResponseEntity<ApiResponse<LedgerDto.IntegrityReportResponse>> verifyIntegrity() {
        LedgerDto.IntegrityReportResponse report = ledgerService.verifyLedgerIntegrity();
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping(value = "/export", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exportLedgerCsv() {
        String csv = ledgerService.exportLedgerReportCsv();
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"so-cai-to-am.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}

