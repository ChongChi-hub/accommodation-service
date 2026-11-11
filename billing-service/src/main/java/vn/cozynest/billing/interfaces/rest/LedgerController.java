package vn.cozynest.billing.interfaces.rest;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.service.LedgerService;
import vn.cozynest.common.dto.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWalletBalance(Authentication authentication) {
        String userId = (authentication != null && authentication.getPrincipal() != null)
                ? (String) authentication.getPrincipal()
                : "user-1";
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
    public ResponseEntity<ApiResponse<List<LedgerDto.AccountResponse>>> getAllAccounts() {
        List<LedgerDto.AccountResponse> list = ledgerService.getAllAccounts();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/integrity")
    public ResponseEntity<ApiResponse<LedgerDto.IntegrityReportResponse>> verifyIntegrity() {
        LedgerDto.IntegrityReportResponse report = ledgerService.verifyLedgerIntegrity();
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * LEDGER-02: Kích hoạt đối soát toàn diện sổ cái ngay lập tức
     * Kiểm tra tổng Nợ = tổng Có theo ngày & theo ví; số dư ví khớp tổng bút toán.
     */
    @PostMapping("/reconcile-now")
    public ResponseEntity<ApiResponse<LedgerDto.ComprehensiveIntegrityReport>> runReconciliation() {
        LedgerDto.ComprehensiveIntegrityReport report = ledgerService.runDailyReconciliation();
        return ResponseEntity.ok(ApiResponse.success("Chạy kiểm tra đối soát toàn diện sổ cái thành công", report));
    }

    @GetMapping(value = "/export", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exportLedgerCsv() {
        String csv = ledgerService.exportLedgerReportCsv();
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"so-cai-tong-hop.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    /**
     * Xuất Sổ Cái Chi Tiết (General Ledger) theo kỳ chuẩn UTF-8 BOM
     */
    @GetMapping(value = "/export/general-ledger", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exportGeneralLedger(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        Instant from = fromDate != null ? fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        Instant to = toDate != null ? toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;

        String csv = ledgerService.exportGeneralLedgerCsv(from, to);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"so-cai-chi-tiet.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    /**
     * Xuất Bảng Cân Đối Thử (Trial Balance) theo kỳ chuẩn UTF-8 BOM
     */
    @GetMapping(value = "/export/trial-balance", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exportTrialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        Instant from = fromDate != null ? fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        Instant to = toDate != null ? toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;

        String csv = ledgerService.exportTrialBalanceCsv(from, to);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bang-can-doi-thu.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    /**
     * Xuất Sao Kê Tài Khoản / Ví theo kỳ chuẩn UTF-8 BOM
     */
    @GetMapping(value = "/export/statement", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exportStatement(
            @RequestParam String accountNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        Instant from = fromDate != null ? fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        Instant to = toDate != null ? toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;

        String csv = ledgerService.exportAccountStatementCsv(accountNumber, from, to);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sao-ke-" + accountNumber + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
