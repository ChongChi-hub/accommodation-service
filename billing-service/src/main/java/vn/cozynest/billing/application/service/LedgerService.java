package vn.cozynest.billing.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.port.AlertNotificationPort;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.JournalEntryRepository;
import vn.cozynest.billing.domain.repository.LedgerAccountRepository;
import vn.cozynest.billing.domain.repository.ReconciliationReportRepository;

import java.time.Instant;
import java.util.*;

@Service
public class LedgerService {
    public static final String ACC_GATEWAY = "1010-GATEWAY";
    public static final String ACC_ESCROW = "2010-ESCROW";
    public static final String ACC_REVENUE = "4010-REVENUE";
    public static final String ACC_EXPENSE = "5010-EXPENSE";

    private final LedgerAccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final LedgerReconciliationService reconciliationService;
    private final LedgerExportService exportService;

    public LedgerService(LedgerAccountRepository accountRepository,
                         JournalEntryRepository journalEntryRepository,
                         AlertNotificationPort alertNotificationPort,
                         ReconciliationReportRepository reconReportRepository) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.reconciliationService = new LedgerReconciliationService(
                accountRepository, journalEntryRepository, alertNotificationPort, reconReportRepository);
        this.exportService = new LedgerExportService(accountRepository, journalEntryRepository);
    }

    @Autowired
    public LedgerService(LedgerAccountRepository accountRepository,
                         JournalEntryRepository journalEntryRepository,
                         LedgerReconciliationService reconciliationService,
                         LedgerExportService exportService) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.reconciliationService = reconciliationService;
        this.exportService = exportService;
    }

    @Transactional
    public LedgerAccount getOrCreateAccount(String accountNumber, String name, AccountType type, String ownerId) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseGet(() -> accountRepository.save(LedgerAccount.create(accountNumber, name, type, ownerId)));
    }

    public String getWalletAccountNumber(String userId) {
        return "2020-WALLET-" + userId;
    }

    @Transactional
    public LedgerAccount getOrCreateWallet(String userId, String userName) {
        String accNum = getWalletAccountNumber(userId);
        return getOrCreateAccount(accNum, "Ví tiền chủ trọ: " + (userName != null ? userName : userId), AccountType.LIABILITY, userId);
    }

    @Transactional
    public JournalEntry postEntry(String description, String refType, String refId, String createdBy,
                                  List<LedgerDto.PostingItem> postingItems) {
        return postEntryIdempotent(null, description, refType, refId, createdBy, postingItems);
    }

    @Transactional
    public JournalEntry postEntryIdempotent(String idempotencyKey, String description, String refType,
                                            String refId, String createdBy,
                                            List<LedgerDto.PostingItem> postingItems) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<JournalEntry> existing = journalEntryRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        // Acquire pessimistic write locks in deterministic lexicographical accountNumber order to prevent deadlocks
        List<String> sortedAccountNumbers = postingItems.stream()
                .map(LedgerDto.PostingItem::accountNumber)
                .distinct()
                .sorted()
                .toList();

        Map<String, LedgerAccount> lockedAccounts = new LinkedHashMap<>();
        for (String accNum : sortedAccountNumbers) {
            LedgerAccount locked = accountRepository.findByAccountNumberForUpdate(accNum)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản sổ cái: " + accNum));
            lockedAccounts.put(accNum, locked);
        }

        String entryId = UUID.randomUUID().toString();
        String entryNumber = "JE-" + System.currentTimeMillis() + "-" + entryId.substring(0, 8);
        List<LedgerPosting> domainPostings = new ArrayList<>();

        for (LedgerDto.PostingItem item : postingItems) {
            LedgerAccount account = lockedAccounts.get(item.accountNumber());
            account.applyPosting(item.direction(), item.amount());
            domainPostings.add(LedgerPosting.create(entryId, account.getId(), item.direction(), item.amount()));
        }

        JournalEntry journalEntry = JournalEntry.createWithIdAndIdempotency(
                entryId, entryNumber, description, refType, refId,
                idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey.trim() : null,
                createdBy, domainPostings);

        for (LedgerAccount acc : lockedAccounts.values()) {
            accountRepository.save(acc);
        }
        return journalEntryRepository.save(journalEntry);
    }

    @Transactional
    public JournalEntry reverseEntry(String originalEntryId, String actorId, String reason) {
        JournalEntry original = journalEntryRepository.findById(originalEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bút toán gốc: " + originalEntryId));
        Map<String, String> idToAccountNumber = new HashMap<>();
        for (LedgerAccount acc : accountRepository.findAll()) {
            idToAccountNumber.put(acc.getId(), acc.getAccountNumber());
        }
        List<LedgerDto.PostingItem> reversedItems = original.getPostings().stream()
                .map(p -> new LedgerDto.PostingItem(
                        Optional.ofNullable(idToAccountNumber.get(p.getAccountId()))
                                .orElseThrow(() -> new IllegalStateException("Thiếu tài khoản: " + p.getAccountId())),
                        p.getDirection() == PostingDirection.DEBIT ? PostingDirection.CREDIT : PostingDirection.DEBIT,
                        p.getAmount()))
                .toList();
        return postEntryIdempotent("REV-" + original.getId(),
                "Đảo bút toán " + original.getEntryNumber() + ": " + reason,
                "REVERSAL", original.getId(), actorId, reversedItems);
    }

    @Transactional(readOnly = true)
    public long getAccountBalance(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).map(LedgerAccount::getBalance).orElse(0L);
    }

    @Transactional(readOnly = true)
    public List<LedgerDto.AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(acc -> new LedgerDto.AccountResponse(acc.getId(), acc.getAccountNumber(),
                        acc.getAccountName(), acc.getAccountType(), acc.getOwnerId(),
                        acc.getBalance(), acc.getCurrency(), acc.isFrozen()))
                .toList();
    }

    public LedgerDto.IntegrityReportResponse verifyLedgerIntegrity() { return reconciliationService.verifyLedgerIntegrity(); }
    public LedgerDto.ComprehensiveIntegrityReport runDailyReconciliation() { return reconciliationService.runDailyReconciliation(); }
    public String exportLedgerReportCsv() { return exportService.exportLedgerReportCsv(); }
    public String exportGeneralLedgerCsv(Instant from, Instant to) { return exportService.exportGeneralLedgerCsv(from, to); }
    public String exportTrialBalanceCsv(Instant from, Instant to) { return exportService.exportTrialBalanceCsv(from, to); }
    public String exportAccountStatementCsv(String accNum, Instant from, Instant to) { return exportService.exportAccountStatementCsv(accNum, from, to); }
}
