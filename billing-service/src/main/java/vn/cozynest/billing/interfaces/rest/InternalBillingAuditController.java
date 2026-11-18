package vn.cozynest.billing.interfaces.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.billing.domain.model.JournalEntry;
import vn.cozynest.billing.domain.model.MakerCheckerRequest;
import vn.cozynest.billing.domain.model.PayoutRequest;
import vn.cozynest.billing.domain.model.RefundRequest;
import vn.cozynest.billing.domain.repository.JournalEntryRepository;
import vn.cozynest.billing.domain.repository.MakerCheckerRepository;
import vn.cozynest.billing.domain.repository.PayoutRequestRepository;
import vn.cozynest.billing.domain.repository.RefundRequestRepository;
import vn.cozynest.common.dto.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/billing")
public class InternalBillingAuditController {

    private static final String DEFAULT_INTERNAL_KEY = "CozyNestInternalServiceKeySecret2026";

    private final MakerCheckerRepository makerCheckerRepository;
    private final PayoutRequestRepository payoutRepository;
    private final RefundRequestRepository refundRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final String expectedServiceKey;

    @Autowired
    public InternalBillingAuditController(
            MakerCheckerRepository makerCheckerRepository,
            PayoutRequestRepository payoutRepository,
            RefundRequestRepository refundRepository,
            JournalEntryRepository journalEntryRepository,
            @Value("${app.internal.service-key:" + DEFAULT_INTERNAL_KEY + "}") String expectedServiceKey,
            @Autowired(required = false) Environment env
    ) {
        this.makerCheckerRepository = makerCheckerRepository;
        this.payoutRepository = payoutRepository;
        this.refundRepository = refundRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.expectedServiceKey = expectedServiceKey;
        if (env != null && env.matchesProfiles("prod", "production", "staging")) {
            if (DEFAULT_INTERNAL_KEY.equals(expectedServiceKey)) {
                throw new IllegalStateException("Restricted environment requires explicit app.internal.service-key");
            }
        }
    }

    @GetMapping("/audit-events")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAuditEvents(
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey
    ) {
        if (serviceKey == null || !isKeyValid(serviceKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Truy cap noi bo khong hop le", "ERR_UNAUTHORIZED"));
        }
        List<Map<String, Object>> events = new ArrayList<>();
        for (MakerCheckerRequest mc : makerCheckerRepository.findAll()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "mc-" + mc.getId());
            row.put("actor", mc.getCheckerId() != null ? mc.getCheckerId() : mc.getMakerId());
            row.put("actorRole", mc.getCheckerId() != null ? "ADMIN" : "MAKER");
            row.put("action", "MAKER_CHECKER_" + mc.getActionType() + "_" + mc.getStatus());
            row.put("target", "MAKER_CHECKER (" + mc.getId() + ") - " + mc.getAmount() + " VND");
            row.put("details", mc.getReason());
            row.put("ip", "system");
            row.put("source", "BILLING");
            row.put("timestamp", (mc.getDecidedAt() != null ? mc.getDecidedAt() : mc.getCreatedAt()).toString());
            events.add(row);
        }
        for (PayoutRequest po : payoutRepository.findAll()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "po-" + po.getId());
            row.put("actor", po.getLandlordId());
            row.put("actorRole", "LANDLORD");
            row.put("action", "PAYOUT_" + po.getStatus());
            row.put("target", "PAYOUT (" + po.getPayoutCode() + ") - " + po.getAmount() + " VND");
            row.put("details", po.getBankCode() + " " + po.getAccountNumber());
            row.put("ip", "system");
            row.put("source", "BILLING");
            row.put("timestamp", (po.getUpdatedAt() != null ? po.getUpdatedAt() : po.getCreatedAt()).toString());
            events.add(row);
        }
        for (RefundRequest rf : refundRepository.findAll()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "rf-" + rf.getId());
            row.put("actor", rf.getTenantId());
            row.put("actorRole", "TENANT");
            row.put("action", "REFUND_" + rf.getStatus());
            row.put("target", "REFUND (" + rf.getRefundCode() + ") - " + rf.getAmount() + " VND");
            row.put("details", rf.getReason());
            row.put("ip", "system");
            row.put("source", "BILLING");
            row.put("timestamp", (rf.getUpdatedAt() != null ? rf.getUpdatedAt() : rf.getCreatedAt()).toString());
            events.add(row);
        }
        for (JournalEntry je : journalEntryRepository.findAll()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "je-" + je.getId());
            row.put("actor", je.getCreatedBy() != null ? je.getCreatedBy() : "SYSTEM_LEDGER");
            row.put("actorRole", "SYSTEM");
            row.put("action", "LEDGER_" + je.getReferenceType());
            row.put("target", "JOURNAL (" + je.getEntryNumber() + ") - " + je.getTotalAmount() + " VND");
            row.put("details", je.getDescription());
            row.put("ip", "system");
            row.put("source", "BILLING");
            row.put("timestamp", je.getCreatedAt().toString());
            events.add(row);
        }
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    private boolean isKeyValid(String serviceKey) {
        if (expectedServiceKey == null) return false;
        return MessageDigest.isEqual(
                serviceKey.getBytes(StandardCharsets.UTF_8),
                expectedServiceKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
