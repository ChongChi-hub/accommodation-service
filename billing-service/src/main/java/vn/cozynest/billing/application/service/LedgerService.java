package vn.cozynest.billing.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.JournalEntryRepository;
import vn.cozynest.billing.domain.repository.LedgerAccountRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerService {
    public static final String ACC_GATEWAY = "1010-GATEWAY";
    public static final String ACC_ESCROW = "2010-ESCROW";
    public static final String ACC_REVENUE = "4010-REVENUE";
    public static final String ACC_EXPENSE = "5010-EXPENSE";

    private final LedgerAccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;

    public LedgerService(LedgerAccountRepository accountRepository,
                         JournalEntryRepository journalEntryRepository) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    @Transactional
    public LedgerAccount getOrCreateAccount(String accountNumber, String name, AccountType type, String ownerId) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseGet(() -> {
                    LedgerAccount acc = LedgerAccount.create(accountNumber, name, type, ownerId);
                    return accountRepository.save(acc);
                });
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
        String entryNumber = "JE-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        String entryId = UUID.randomUUID().toString();

        List<LedgerPosting> domainPostings = new ArrayList<>();
        List<LedgerAccount> accountsToUpdate = new ArrayList<>();

        for (LedgerDto.PostingItem item : postingItems) {
            LedgerAccount account = accountRepository.findByAccountNumber(item.accountNumber())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản sổ cái: " + item.accountNumber()));

            account.applyPosting(item.direction(), item.amount());
            accountsToUpdate.add(account);

            domainPostings.add(LedgerPosting.create(entryId, account.getId(), item.direction(), item.amount()));
        }

        // Validate double-entry invariant: Sum(DEBIT) == Sum(CREDIT)
        JournalEntry journalEntry = JournalEntry.createAndValidate(
                entryNumber, description, refType, refId, createdBy, domainPostings
        );

        // Persist updated account balances
        for (LedgerAccount acc : accountsToUpdate) {
            accountRepository.save(acc);
        }

        return journalEntryRepository.save(journalEntry);
    }

    @Transactional(readOnly = true)
    public long getAccountBalance(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(LedgerAccount::getBalance)
                .orElse(0L);
    }

    @Transactional(readOnly = true)
    public LedgerDto.IntegrityReportResponse verifyLedgerIntegrity() {
        List<JournalEntry> entries = journalEntryRepository.findAll();
        long totalDebit = 0;
        long totalCredit = 0;

        for (JournalEntry entry : entries) {
            for (LedgerPosting p : entry.getPostings()) {
                if (p.getDirection() == PostingDirection.DEBIT) {
                    totalDebit += p.getAmount();
                } else {
                    totalCredit += p.getAmount();
                }
            }
        }

        boolean isBalanced = (totalDebit == totalCredit);
        List<LedgerAccount> accounts = accountRepository.findAll();

        return new LedgerDto.IntegrityReportResponse(
                isBalanced,
                totalDebit,
                totalCredit,
                accounts.size(),
                isBalanced ? "Sổ cái cân đối hoàn toàn (Tổng Nợ = Tổng Có)" : "CẢNH BÁO: Sổ cái bị lệch Nợ/Có!"
        );
    }

    @Transactional(readOnly = true)
    public String exportLedgerReportCsv() {
        List<LedgerAccount> accounts = accountRepository.findAll();
        StringBuilder sb = new StringBuilder("\uFEFF"); // UTF-8 BOM for Excel
        sb.append("Mã tài khoản,Tên tài khoản,Loại tài khoản,Số dư (VND),Đóng băng\n");
        for (LedgerAccount acc : accounts) {
            sb.append('"').append(acc.getAccountNumber()).append("\",")
                    .append('"').append(acc.getAccountName()).append("\",")
                    .append('"').append(acc.getAccountType()).append("\",")
                    .append(acc.getBalance()).append(",")
                    .append(acc.isFrozen() ? "Đã khóa" : "Hoạt động")
                    .append("\n");
        }
        return sb.toString();
    }
}

