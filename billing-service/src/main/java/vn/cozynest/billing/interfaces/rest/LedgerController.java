package vn.cozynest.billing.interfaces.rest;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.service.LedgerService;
import vn.cozynest.common.dto.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing/ledger")
public class LedgerController {
    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/wallet")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWalletBalance(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("Yeu cau dang nhap");
        }
        String userId = String.valueOf(authentication.getPrincipal());
        String walletAcc = ledgerService.getWalletAccountNumber(userId);
        long balance = ledgerService.getAccountBalance(walletAcc);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "userId", userId,
                "accountNumber", walletAcc,
                "balance", balance,
                "currency", "VND"
        )));
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<LedgerDto.AccountResponse>>> getAllAccounts() {
        return ResponseEntity.ok(ApiResponse.success(ledgerService.getAllAccounts()));
    }

    @GetMapping("/integrity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LedgerDto.IntegrityReportResponse>> verifyIntegrity() {
        return ResponseEntity.ok(ApiResponse.success(ledgerService.verifyLedgerIntegrity()));
    }

    @PostMapping("/reconcile-now")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LedgerDto.ComprehensiveIntegrityReport>> runReconciliation() {
        return ResponseEntity.ok(ApiResponse.success(
                "Chạy kiểm tra đối soát toàn diện sổ cái thành công",
                ledgerService.runDailyReconciliation()));
    }

    @GetMapping(value = "/export", produces = "text/csv; charset=UTF-8")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportLedgerCsv() {
        return csvResponse("so-cai-tong-hop.csv", ledgerService.exportLedgerReportCsv());
    }

    @GetMapping(value = "/export/general-ledger", produces = "text/csv; charset=UTF-8")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportGeneralLedger(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        Instant[] range = toUtcRange(fromDate, toDate);
        return csvResponse("so-cai-chi-tiet.csv", ledgerService.exportGeneralLedgerCsv(range[0], range[1]));
    }

    @GetMapping(value = "/export/trial-balance", produces = "text/csv; charset=UTF-8")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportTrialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        Instant[] range = toUtcRange(fromDate, toDate);
        return csvResponse("bang-can-doi-thu.csv", ledgerService.exportTrialBalanceCsv(range[0], range[1]));
    }

    @GetMapping(value = "/export/statement", produces = "text/csv; charset=UTF-8")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportStatement(
            @RequestParam String accountNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        Instant[] range = toUtcRange(fromDate, toDate);
        return csvResponse("sao-ke-" + accountNumber + ".csv",
                ledgerService.exportAccountStatementCsv(accountNumber, range[0], range[1]));
    }

    private Instant[] toUtcRange(LocalDate fromDate, LocalDate toDate) {
        return new Instant[]{
                fromDate != null ? fromDate.atStartOfDay(ZoneOffset.UTC).toInstant() : null,
                toDate != null ? toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null
        };
    }

    private ResponseEntity<byte[]> csvResponse(String filename, String csv) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
