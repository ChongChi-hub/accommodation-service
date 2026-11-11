package vn.cozynest.billing.application.dto;

import vn.cozynest.billing.domain.model.AccountType;
import vn.cozynest.billing.domain.model.PostingDirection;

import java.time.Instant;
import java.util.List;

public class LedgerDto {

    public record AccountResponse(
            String id,
            String accountNumber,
            String accountName,
            AccountType accountType,
            String ownerId,
            long balance,
            String currency,
            boolean isFrozen
    ) {}

    public record PostingItem(
            String accountNumber,
            PostingDirection direction,
            long amount
    ) {}

    public record JournalEntryRequest(
            String description,
            String referenceType,
            String referenceId,
            List<PostingItem> postings
    ) {}

    public record JournalEntryResponse(
            String id,
            String entryNumber,
            String description,
            String referenceType,
            String referenceId,
            long totalAmount,
            String status,
            Instant createdAt
    ) {}

    public record IntegrityReportResponse(
            boolean isBalanced,
            long totalDebit,
            long totalCredit,
            int totalAccounts,
            String message
    ) {}

    public record DailyReconciliationItem(
            String date,
            long totalDebit,
            long totalCredit,
            boolean isBalanced,
            long difference,
            int entryCount
    ) {}

    public record WalletReconciliationItem(
            String accountNumber,
            String accountName,
            String accountType,
            String ownerId,
            long storedBalance,
            long calculatedBalance,
            boolean isMatched,
            long difference
    ) {}

    public record ComprehensiveIntegrityReport(
            boolean isSystemBalanced,
            long totalDebit,
            long totalCredit,
            long systemDifference,
            int totalAccounts,
            int totalMismatchedAccounts,
            List<DailyReconciliationItem> dailyBreakdowns,
            List<WalletReconciliationItem> walletBreakdowns,
            boolean alertSent,
            String message
    ) {}

    public record CreateAdjustmentRequest(
            String accountNumber,
            String contraAccountNumber,
            PostingDirection direction,
            long amount,
            String reason
    ) {}

    public record AdjustmentResponse(
            String id,
            String makerId,
            String checkerId,
            String actionType,
            String accountNumber,
            String contraAccountNumber,
            String direction,
            long amount,
            String status,
            String reason,
            Instant decidedAt,
            Instant createdAt
    ) {}

    public record TrialBalanceItem(
            String accountNumber,
            String accountName,
            AccountType accountType,
            long openingDebit,
            long openingCredit,
            long periodDebit,
            long periodCredit,
            long closingDebit,
            long closingCredit
    ) {}
}
