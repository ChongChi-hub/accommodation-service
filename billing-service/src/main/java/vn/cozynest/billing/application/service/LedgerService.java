package vn.cozynest.billing.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.port.AlertNotificationPort;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.JournalEntryRepository;
import vn.cozynest.billing.domain.repository.LedgerAccountRepository;
import vn.cozynest.billing.domain.repository.ReconciliationReportRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LedgerService {
    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    public static final String ACC_GATEWAY = "1010-GATEWAY";
    public static final String ACC_ESCROW = "2010-ESCROW";
    public static final String ACC_REVENUE = "4010-REVENUE";
    public static final String ACC_EXPENSE = "5010-EXPENSE";

    private final LedgerAccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final AlertNotificationPort alertNotificationPort;
    private final ReconciliationReportRepository reconReportRepository;

    public LedgerService(LedgerAccountRepository accountRepository,
                          JournalEntryRepository journalEntryRepository,
                          AlertNotificationPort alertNotificationPort,
                          ReconciliationReportRepository reconReportRepository) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.alertNotificationPort = alertNotificationPort;
        this.reconReportRepository = reconReportRepository;
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
    public List<LedgerDto.AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(acc -> new LedgerDto.AccountResponse(
                        acc.getId(),
                        acc.getAccountNumber(),
                        acc.getAccountName(),
                        acc.getAccountType(),
                        acc.getOwnerId(),
                        acc.getBalance(),
                        acc.getCurrency(),
                        acc.isFrozen()
                ))
                .toList();
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

    /**
     * LEDGER-02: Toàn vẹn sổ cái đối soát toàn diện hằng ngày
     * 1. Kiểm tra tổng Nợ = tổng Có theo từng ngày
     * 2. Kiểm tra theo từng ví / tài khoản: số dư ví khớp tổng các bút toán phát sinh
     * 3. Tự động cảnh báo Telegram / Slack nếu phát hiện lệch
     * 4. Ghi nhận báo cáo đối soát vào database
     */
    @Transactional
    public LedgerDto.ComprehensiveIntegrityReport runDailyReconciliation() {
        List<JournalEntry> allEntries = journalEntryRepository.findAll();
        List<LedgerAccount> allAccounts = accountRepository.findAll();
        Map<String, LedgerAccount> accountMap = allAccounts.stream()
                .collect(Collectors.toMap(LedgerAccount::getId, a -> a));

        // 1. Phân tích đối soát theo từng ngày
        Map<LocalDate, List<LedgerPosting>> postingsByDate = new TreeMap<>();
        Map<LocalDate, Integer> entryCountByDate = new TreeMap<>();

        long totalDebitAll = 0;
        long totalCreditAll = 0;

        for (JournalEntry entry : allEntries) {
            LocalDate date = entry.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            entryCountByDate.merge(date, 1, Integer::sum);
            for (LedgerPosting p : entry.getPostings()) {
                postingsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(p);
                if (p.getDirection() == PostingDirection.DEBIT) {
                    totalDebitAll += p.getAmount();
                } else {
                    totalCreditAll += p.getAmount();
                }
            }
        }

        List<LedgerDto.DailyReconciliationItem> dailyBreakdowns = new ArrayList<>();
        boolean allDaysBalanced = true;

        for (Map.Entry<LocalDate, List<LedgerPosting>> entry : postingsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<LedgerPosting> postings = entry.getValue();

            long dayDebit = 0;
            long dayCredit = 0;
            for (LedgerPosting p : postings) {
                if (p.getDirection() == PostingDirection.DEBIT) {
                    dayDebit += p.getAmount();
                } else {
                    dayCredit += p.getAmount();
                }
            }

            boolean dayBalanced = (dayDebit == dayCredit);
            if (!dayBalanced) {
                allDaysBalanced = false;
            }

            dailyBreakdowns.add(new LedgerDto.DailyReconciliationItem(
                    date.toString(),
                    dayDebit,
                    dayCredit,
                    dayBalanced,
                    Math.abs(dayDebit - dayCredit),
                    entryCountByDate.getOrDefault(date, 0)
            ));
        }

        // 2. Phân tích đối soát theo từng tài khoản / ví (số dư ví khớp tổng bút toán)
        List<LedgerDto.WalletReconciliationItem> walletBreakdowns = new ArrayList<>();
        int mismatchedWalletsCount = 0;

        // Map posting theo accountId
        Map<String, List<LedgerPosting>> postingsByAccount = new HashMap<>();
        for (JournalEntry entry : allEntries) {
            for (LedgerPosting p : entry.getPostings()) {
                postingsByAccount.computeIfAbsent(p.getAccountId(), k -> new ArrayList<>()).add(p);
            }
        }

        for (LedgerAccount acc : allAccounts) {
            List<LedgerPosting> postings = postingsByAccount.getOrDefault(acc.getId(), List.of());
            long sumDebit = 0;
            long sumCredit = 0;
            for (LedgerPosting p : postings) {
                if (p.getDirection() == PostingDirection.DEBIT) {
                    sumDebit += p.getAmount();
                } else {
                    sumCredit += p.getAmount();
                }
            }

            long calculatedBalance;
            if (acc.getAccountType() == AccountType.ASSET || acc.getAccountType() == AccountType.EXPENSE) {
                calculatedBalance = sumDebit - sumCredit;
            } else {
                // LIABILITY, EQUITY, REVENUE: Số dư bình thường là Có (Credit)
                calculatedBalance = sumCredit - sumDebit;
            }

            long storedBalance = acc.getBalance();
            boolean isMatched = (storedBalance == calculatedBalance);
            long diff = Math.abs(storedBalance - calculatedBalance);

            if (!isMatched) {
                mismatchedWalletsCount++;
            }

            walletBreakdowns.add(new LedgerDto.WalletReconciliationItem(
                    acc.getAccountNumber(),
                    acc.getAccountName(),
                    acc.getAccountType().name(),
                    acc.getOwnerId(),
                    storedBalance,
                    calculatedBalance,
                    isMatched,
                    diff
            ));
        }

        boolean isSystemBalanced = (totalDebitAll == totalCreditAll) && allDaysBalanced && (mismatchedWalletsCount == 0);
        long systemDiff = Math.abs(totalDebitAll - totalCreditAll);

        // 3. DoD: Lệch sẽ cảnh báo Telegram / Slack
        boolean alertSent = false;
        String statusMessage;
        if (!isSystemBalanced) {
            String alertBody = String.format(
                    "Sổ cái bị mất cân đối! Tổng Nợ: %,d VND, Tổng Có: %,d VND, Lệch hệ thống: %,d VND. Có %d tài khoản/ví bị lệch số dư.",
                    totalDebitAll, totalCreditAll, systemDiff, mismatchedWalletsCount
            );
            alertSent = alertNotificationPort.sendAlert("🚨 CẢNH BÁO TOÀN VẸN SỔ CÁI COZYNEST", alertBody);
            statusMessage = "CẢNH BÁO: Sổ cái bị lệch Nợ/Có hoặc số dư ví không khớp bút toán!";
        } else {
            statusMessage = "Sổ cái hoàn toàn cân đối: Tổng Nợ = Tổng Có, 100% số dư ví khớp chính xác với lịch sử bút toán.";
        }

        // 4. Lưu bản ghi đối soát vào bảng reconciliation_reports
        try {
            LocalDate today = LocalDate.now();
            Optional<ReconciliationReport> existing = reconReportRepository.findByReconDate(today);
            ReconciliationReport report;
            if (existing.isPresent()) {
                report = existing.get();
                report.update(allEntries.size(), allAccounts.size() - mismatchedWalletsCount,
                        mismatchedWalletsCount, isSystemBalanced ? "COMPLETED" : "MISMATCHED", statusMessage);
            } else {
                report = ReconciliationReport.create(
                        today,
                        allEntries.size(),
                        allAccounts.size() - mismatchedWalletsCount,
                        mismatchedWalletsCount,
                        isSystemBalanced ? "COMPLETED" : "MISMATCHED",
                        statusMessage
                );
            }
            reconReportRepository.save(report);
        } catch (Exception e) {
            log.warn("Không thể lưu bản ghi reconciliation_reports: {}", e.getMessage());
        }

        return new LedgerDto.ComprehensiveIntegrityReport(
                isSystemBalanced,
                totalDebitAll,
                totalCreditAll,
                systemDiff,
                allAccounts.size(),
                mismatchedWalletsCount,
                dailyBreakdowns,
                walletBreakdowns,
                alertSent,
                statusMessage
        );
    }

    /**
     * Xuất danh sách tài khoản cơ bản kèm UTF-8 BOM
     */
    @Transactional(readOnly = true)
    public String exportLedgerReportCsv() {
        List<LedgerAccount> accounts = accountRepository.findAll();
        StringBuilder sb = new StringBuilder("\uFEFF"); // UTF-8 BOM for Excel
        sb.append("Mã tài khoản,Tên tài khoản,Loại tài khoản,Số dư (VND),Đóng băng\n");
        for (LedgerAccount acc : accounts) {
            sb.append('"').append(escapeCsv(acc.getAccountNumber())).append("\",")
                    .append('"').append(escapeCsv(acc.getAccountName())).append("\",")
                    .append('"').append(acc.getAccountType()).append("\",")
                    .append(acc.getBalance()).append(",")
                    .append(acc.isFrozen() ? "Đã khóa" : "Hoạt động")
                    .append("\n");
        }
        return sb.toString();
    }

    /**
     * Xuất Sổ Cái Chi Tiết (General Ledger CSV) theo khoảng thời gian chuẩn UTF-8 BOM
     */
    @Transactional(readOnly = true)
    public String exportGeneralLedgerCsv(Instant from, Instant to) {
        List<JournalEntry> allEntries = journalEntryRepository.findAll();
        List<LedgerAccount> allAccounts = accountRepository.findAll();
        Map<String, LedgerAccount> accountMap = allAccounts.stream()
                .collect(Collectors.toMap(LedgerAccount::getId, a -> a));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("Mã bút toán,Ngày giờ hạch toán,Mã tài khoản,Tên tài khoản,Diễn giải,Loại nghiệp vụ,Mã tham chiếu,Nợ (VND),Có (VND),Người tạo\n");

        for (JournalEntry entry : allEntries) {
            Instant created = entry.getCreatedAt();
            if (from != null && created.isBefore(from)) continue;
            if (to != null && created.isAfter(to)) continue;

            for (LedgerPosting p : entry.getPostings()) {
                LedgerAccount acc = accountMap.get(p.getAccountId());
                String accNum = acc != null ? acc.getAccountNumber() : p.getAccountId();
                String accName = acc != null ? acc.getAccountName() : "N/A";

                long debit = p.getDirection() == PostingDirection.DEBIT ? p.getAmount() : 0L;
                long credit = p.getDirection() == PostingDirection.CREDIT ? p.getAmount() : 0L;

                sb.append('"').append(escapeCsv(entry.getEntryNumber())).append("\",")
                        .append('"').append(dtf.format(created)).append("\",")
                        .append('"').append(escapeCsv(accNum)).append("\",")
                        .append('"').append(escapeCsv(accName)).append("\",")
                        .append('"').append(escapeCsv(entry.getDescription())).append("\",")
                        .append('"').append(escapeCsv(entry.getReferenceType())).append("\",")
                        .append('"').append(escapeCsv(entry.getReferenceId() != null ? entry.getReferenceId() : "")).append("\",")
                        .append(debit).append(",")
                        .append(credit).append(",")
                        .append('"').append(escapeCsv(entry.getCreatedBy())).append("\"\n");
            }
        }
        return sb.toString();
    }

    /**
     * Xuất Bảng Cân Đối Thử (Trial Balance CSV) theo kỳ chuẩn UTF-8 BOM
     */
    @Transactional(readOnly = true)
    public String exportTrialBalanceCsv(Instant from, Instant to) {
        List<JournalEntry> allEntries = journalEntryRepository.findAll();
        List<LedgerAccount> allAccounts = accountRepository.findAll();

        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("Mã tài khoản,Tên tài khoản,Loại tài khoản,Dư Nợ đầu kỳ (VND),Dư Có đầu kỳ (VND),Phát sinh Nợ (VND),Phát sinh Có (VND),Dư Nợ cuối kỳ (VND),Dư Có cuối kỳ (VND)\n");

        long totalPeriodDebit = 0;
        long totalPeriodCredit = 0;
        long totalClosingDebit = 0;
        long totalClosingCredit = 0;

        for (LedgerAccount acc : allAccounts) {
            long openingDebit = 0;
            long openingCredit = 0;
            long periodDebit = 0;
            long periodCredit = 0;

            for (JournalEntry entry : allEntries) {
                Instant created = entry.getCreatedAt();
                for (LedgerPosting p : entry.getPostings()) {
                    if (p.getAccountId().equals(acc.getId())) {
                        boolean isBefore = (from != null && created.isBefore(from));
                        boolean isInPeriod = (from == null || !created.isBefore(from)) && (to == null || !created.isAfter(to));

                        if (isBefore) {
                            if (p.getDirection() == PostingDirection.DEBIT) openingDebit += p.getAmount();
                            else openingCredit += p.getAmount();
                        } else if (isInPeriod) {
                            if (p.getDirection() == PostingDirection.DEBIT) periodDebit += p.getAmount();
                            else periodCredit += p.getAmount();
                        }
                    }
                }
            }

            // Tính số dư đầu kỳ thuần
            long netOpeningDebit = 0;
            long netOpeningCredit = 0;
            if (acc.getAccountType() == AccountType.ASSET || acc.getAccountType() == AccountType.EXPENSE) {
                long net = openingDebit - openingCredit;
                if (net >= 0) netOpeningDebit = net;
                else netOpeningCredit = -net;
            } else {
                long net = openingCredit - openingDebit;
                if (net >= 0) netOpeningCredit = net;
                else netOpeningDebit = -net;
            }

            // Tính số dư cuối kỳ
            long closingDebit = 0;
            long closingCredit = 0;
            long totalDebitAcc = openingDebit + periodDebit;
            long totalCreditAcc = openingCredit + periodCredit;

            if (acc.getAccountType() == AccountType.ASSET || acc.getAccountType() == AccountType.EXPENSE) {
                long net = totalDebitAcc - totalCreditAcc;
                if (net >= 0) closingDebit = net;
                else closingCredit = -net;
            } else {
                long net = totalCreditAcc - totalDebitAcc;
                if (net >= 0) closingCredit = net;
                else closingDebit = -net;
            }

            totalPeriodDebit += periodDebit;
            totalPeriodCredit += periodCredit;
            totalClosingDebit += closingDebit;
            totalClosingCredit += closingCredit;

            sb.append('"').append(escapeCsv(acc.getAccountNumber())).append("\",")
                    .append('"').append(escapeCsv(acc.getAccountName())).append("\",")
                    .append('"').append(acc.getAccountType()).append("\",")
                    .append(netOpeningDebit).append(",")
                    .append(netOpeningCredit).append(",")
                    .append(periodDebit).append(",")
                    .append(periodCredit).append(",")
                    .append(closingDebit).append(",")
                    .append(closingCredit).append("\n");
        }

        // Hàng tổng kết cân đối
        sb.append("\"TỔNG CỘNG\",\"Cân đối toàn hệ thống\",\"TOTAL\",0,0,")
                .append(totalPeriodDebit).append(",")
                .append(totalPeriodCredit).append(",")
                .append(totalClosingDebit).append(",")
                .append(totalClosingCredit).append("\n");

        return sb.toString();
    }

    /**
     * Xuất Sao Kê Tài Khoản / Ví (Account Statement CSV) theo kỳ chuẩn UTF-8 BOM
     */
    @Transactional(readOnly = true)
    public String exportAccountStatementCsv(String accountNumber, Instant from, Instant to) {
        LedgerAccount acc = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản: " + accountNumber));

        List<JournalEntry> allEntries = journalEntryRepository.findAll();
        allEntries.sort(Comparator.comparing(JournalEntry::getCreatedAt));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("Sao kê tài khoản: ").append(acc.getAccountNumber()).append(" - ").append(acc.getAccountName()).append("\n");
        sb.append("Ngày giờ,Mã bút toán,Diễn giải,Nợ (VND),Có (VND),Số dư lũy kế (VND),Mã tham chiếu\n");

        long runningBalance = 0;

        for (JournalEntry entry : allEntries) {
            for (LedgerPosting p : entry.getPostings()) {
                if (p.getAccountId().equals(acc.getId())) {
                    long debit = (p.getDirection() == PostingDirection.DEBIT) ? p.getAmount() : 0L;
                    long credit = (p.getDirection() == PostingDirection.CREDIT) ? p.getAmount() : 0L;

                    if (acc.getAccountType() == AccountType.ASSET || acc.getAccountType() == AccountType.EXPENSE) {
                        runningBalance += (debit - credit);
                    } else {
                        runningBalance += (credit - debit);
                    }

                    Instant created = entry.getCreatedAt();
                    if (from != null && created.isBefore(from)) continue;
                    if (to != null && created.isAfter(to)) continue;

                    sb.append('"').append(dtf.format(created)).append("\",")
                            .append('"').append(escapeCsv(entry.getEntryNumber())).append("\",")
                            .append('"').append(escapeCsv(entry.getDescription())).append("\",")
                            .append(debit).append(",")
                            .append(credit).append(",")
                            .append(runningBalance).append(",")
                            .append('"').append(escapeCsv(entry.getReferenceId() != null ? entry.getReferenceId() : "")).append("\"\n");
                }
            }
        }

        return sb.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }
}
