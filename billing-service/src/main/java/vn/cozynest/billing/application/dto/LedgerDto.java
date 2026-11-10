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
}

