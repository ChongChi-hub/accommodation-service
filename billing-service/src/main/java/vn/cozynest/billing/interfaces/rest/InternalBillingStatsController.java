package vn.cozynest.billing.interfaces.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.cozynest.common.dto.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/stats")
public class InternalBillingStatsController {

    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final JdbcTemplate jdbcTemplate;
    private final String expectedServiceKey;
    private final String fintechMode;

    @Autowired
    public InternalBillingStatsController(
            JdbcTemplate jdbcTemplate,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String expectedServiceKey,
            @Value("${app.fintech.mode:SANDBOX_ONLY}") String fintechMode,
            @Autowired(required = false) Environment env
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.expectedServiceKey = expectedServiceKey;
        this.fintechMode = fintechMode;
        if (env != null && env.matchesProfiles("prod", "production", "staging")) {
            if (DEFAULT_INTERNAL_KEY.equals(expectedServiceKey)) {
                throw new IllegalStateException("Restricted environment requires explicit app.internal.service-key");
            }
        }
    }

    @GetMapping("/billing")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBillingStats(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey
    ) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid internal service key", "ERR_UNAUTHORIZED"));
        }

        LocalDate toDate = (to != null && !to.isBlank()) ? LocalDate.parse(to) : LocalDate.now();
        LocalDate fromDate = (from != null && !from.isBlank()) ? LocalDate.parse(from) : toDate.minusDays(29);

        Map<String, Long> invoicesByStatus = new LinkedHashMap<>();
        long totalInvoices = 0L;
        long paidInvoices = 0L;
        long paidRevenue = 0L;
        long totalInvoicedAmount = 0L;

        List<Map<String, Object>> invRows = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) AS cnt, COALESCE(SUM(total_amount), 0) AS amt FROM invoices GROUP BY status ORDER BY status"
        );
        for (Map<String, Object> row : invRows) {
            String status = String.valueOf(row.get("status"));
            long cnt = ((Number) row.get("cnt")).longValue();
            long amt = ((Number) row.get("amt")).longValue();
            invoicesByStatus.put(status, cnt);
            totalInvoices += cnt;
            totalInvoicedAmount += amt;
            if ("PAID".equalsIgnoreCase(status)) {
                paidInvoices = cnt;
                paidRevenue = amt;
            }
        }

        List<Map<String, Object>> dailyRevenue = new ArrayList<>();
        List<Map<String, Object>> tsRows = jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(COALESCE(updated_at, created_at), '%Y-%m-%d') AS bucket, " +
                "COALESCE(SUM(total_amount), 0) AS amt, COUNT(*) AS cnt " +
                "FROM invoices WHERE deleted_at IS NULL AND status = 'PAID' AND DATE(COALESCE(updated_at, created_at)) BETWEEN ? AND ? " +
                "GROUP BY bucket ORDER BY bucket",
                fromDate.toString(), toDate.toString()
        );
        for (Map<String, Object> row : tsRows) {
            dailyRevenue.add(Map.of(
                    "date", String.valueOf(row.get("bucket")),
                    "amount", ((Number) row.get("amt")).longValue(),
                    "count", ((Number) row.get("cnt")).longValue()
            ));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("totalInvoices", totalInvoices);
        payload.put("paidInvoices", paidInvoices);
        payload.put("paidRevenue", paidRevenue);
        payload.put("totalInvoicedAmount", totalInvoicedAmount);
        payload.put("invoicesByStatus", invoicesByStatus);
        payload.put("dailyRevenue", dailyRevenue);
        payload.put("fintechMode", fintechMode);
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    private boolean isKeyValid(String serviceKey) {
        if (expectedServiceKey == null) return false;
        return MessageDigest.isEqual(
                serviceKey.getBytes(StandardCharsets.UTF_8),
                expectedServiceKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
