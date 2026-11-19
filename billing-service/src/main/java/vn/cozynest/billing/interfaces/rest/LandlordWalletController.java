package vn.cozynest.billing.interfaces.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.service.EscrowService;
import vn.cozynest.billing.application.service.LedgerService;
import vn.cozynest.billing.domain.model.EscrowHold;
import vn.cozynest.common.dto.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing/wallet")
@PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
public class LandlordWalletController {
    private final LedgerService ledgerService;
    private final EscrowService escrowService;
    private final int commissionPercent;
    private final int depositReleasePercent;
    private final int withdrawalHoldDays;
    private final long withdrawalMinAmount;

    @Autowired
    public LandlordWalletController(
            LedgerService ledgerService,
            EscrowService escrowService,
            @Value("${fintech.commission.percent:${commission.percent:5}}") int commissionPercent,
            @Value("${fintech.deposit.release-percent:${deposit.release.percent:50}}") int depositReleasePercent,
            @Value("${fintech.withdrawal.hold-days:${withdrawal.hold.days:0}}") int withdrawalHoldDays,
            @Value("${fintech.withdrawal.min-amount:${withdrawal.min.amount:1000}}") long withdrawalMinAmount) {
        this.ledgerService = ledgerService;
        this.escrowService = escrowService;
        this.commissionPercent = commissionPercent;
        this.depositReleasePercent = depositReleasePercent;
        this.withdrawalHoldDays = withdrawalHoldDays;
        this.withdrawalMinAmount = withdrawalMinAmount;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLandlordWalletSummary(Authentication auth) {
        String landlordId = String.valueOf(auth.getPrincipal());
        long availableBalance = ledgerService.getAccountBalance(ledgerService.getLandlordAvailableAccountNumber(landlordId));
        long pendingBalance = ledgerService.getAccountBalance(ledgerService.getLandlordPendingAccountNumber(landlordId));
        long withdrawalHoldBalance = ledgerService.getAccountBalance(ledgerService.getWithdrawalHoldAccountNumber(landlordId));
        List<EscrowHold> holds = escrowService.getHoldsForLandlord(landlordId);
        long escrowRetainedBalance = holds.stream()
                .filter(h -> "HELD".equals(h.getStatus()) || "PARTIALLY_RELEASED".equals(h.getStatus()) || "DISPUTE_FROZEN".equals(h.getStatus()))
                .mapToLong(EscrowHold::getEffectiveRetainedAmount)
                .sum();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "availableBalance", availableBalance,
                "pendingBalance", pendingBalance,
                "withdrawalHoldBalance", withdrawalHoldBalance,
                "escrowRetainedBalance", escrowRetainedBalance,
                "commissionPercent", commissionPercent,
                "depositReleasePercent", depositReleasePercent,
                "withdrawalHoldDays", withdrawalHoldDays,
                "withdrawalMinAmount", withdrawalMinAmount
        )));
    }

    @GetMapping("/ledger")
    public ResponseEntity<ApiResponse<List<LedgerDto.JournalEntryDetailResponse>>> getLandlordLedgerHistory(
            Authentication auth,
            @RequestParam(defaultValue = "100") int limit) {
        String landlordId = String.valueOf(auth.getPrincipal());
        String availAcc = ledgerService.getLandlordAvailableAccountNumber(landlordId);
        String pendAcc = ledgerService.getLandlordPendingAccountNumber(landlordId);
        String holdAcc = ledgerService.getWithdrawalHoldAccountNumber(landlordId);
        List<LedgerDto.JournalEntryDetailResponse> entries = ledgerService.getRecentJournalEntries(Math.max(50, limit)).stream()
                .filter(je -> je.postings().stream().anyMatch(p ->
                        availAcc.equals(p.accountNumber()) || pendAcc.equals(p.accountNumber()) || holdAcc.equals(p.accountNumber())))
                .limit(Math.max(1, Math.min(limit, 200)))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(entries));
    }
}
