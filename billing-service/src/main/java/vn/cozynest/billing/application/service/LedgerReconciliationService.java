package vn.cozynest.billing.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.port.AlertNotificationPort;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.JournalEntryRepository;
import vn.cozynest.billing.domain.repository.LedgerAccountRepository;
import vn.cozynest.billing.domain.repository.LedgerDailySnapshotRepository;
import vn.cozynest.billing.domain.repository.ReconciliationReportRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class LedgerReconciliationService {
    private static final Logger log = LoggerFactory.getLogger(LedgerReconciliationService.class);

    private final LedgerAccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final AlertNotificationPort alertNotificationPort;
    private final ReconciliationReportRepository reconReportRepository;
    private final LedgerDailySnapshotRepository snapshotRepository;

    public LedgerReconciliationService(LedgerAccountRepository accountRepository,
                                       JournalEntryRepository journalEntryRepository,
                                       AlertNotificationPort alertNotificationPort,
                                       ReconciliationReportRepository reconReportRepository) {
        this(accountRepository, journalEntryRepository, alertNotificationPort, reconReportRepository, null);
    }

    @Autowired
    public LedgerReconciliationService(LedgerAccountRepository accountRepository,
                                       JournalEntryRepository journalEntryRepository,
                                       AlertNotificationPort alertNotificationPort,
                                       ReconciliationReportRepository reconReportRepository,
                                       LedgerDailySnapshotRepository snapshotRepository) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.alertNotificationPort = alertNotificationPort;
        this.reconReportRepository = reconReportRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional(readOnly = true)
    public LedgerDto.IntegrityReportResponse verifyLedgerIntegrity() {
        List<JournalEntry> entries = journalEntryRepository.findAll();
        long totalDebit = 0;
        long totalCredit = 0;
        for (JournalEntry entry : entries) {
            for (LedgerPosting p : entry.getPostings()) {
                if (p.getDirection() == PostingDirection.DEBIT) totalDebit += p.getAmount();
                else totalCredit += p.getAmount();
            }
        }
        boolean isBalanced = (totalDebit == totalCredit);
        List<LedgerAccount> accounts = accountRepository.findAll();
        return new LedgerDto.IntegrityReportResponse(
                isBalanced, totalDebit, totalCredit, accounts.size(),
                isBalanced ? "Sổ cái cân đối hoàn toàn (Tổng Nợ = Tổng Có)" : "CẢNH BÁO: Sổ cái bị lệch Nợ/Có!"
        );
    }

    @Transactional
    public LedgerDto.ComprehensiveIntegrityReport runDailyReconciliation() {
        List<JournalEntry> allEntries = journalEntryRepository.findAll();
        List<LedgerAccount> allAccounts = accountRepository.findAll();

        Map<LocalDate, long[]> dayTotals = new TreeMap<>();
        Map<LocalDate, Integer> dayCounts = new TreeMap<>();
        long totalDebitAll = 0;
        long totalCreditAll = 0;

        for (JournalEntry entry : allEntries) {
            LocalDate date = entry.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            dayCounts.merge(date, 1, Integer::sum);
            long[] dc = dayTotals.computeIfAbsent(date, k -> new long[2]);
            for (LedgerPosting p : entry.getPostings()) {
                if (p.getDirection() == PostingDirection.DEBIT) {
                    dc[0] += p.getAmount();
                    totalDebitAll += p.getAmount();
                } else {
                    dc[1] += p.getAmount();
                    totalCreditAll += p.getAmount();
                }
            }
        }

        List<LedgerDto.DailyReconciliationItem> dailyBreakdowns = new ArrayList<>();
        boolean allDaysBalanced = true;
        for (Map.Entry<LocalDate, long[]> e : dayTotals.entrySet()) {
            long dayDebit = e.getValue()[0];
            long dayCredit = e.getValue()[1];
            boolean dayBalanced = (dayDebit == dayCredit);
            if (!dayBalanced) allDaysBalanced = false;
            dailyBreakdowns.add(new LedgerDto.DailyReconciliationItem(
                    e.getKey().toString(), dayDebit, dayCredit, dayBalanced,
                    Math.abs(dayDebit - dayCredit), dayCounts.getOrDefault(e.getKey(), 0)));
        }

        Map<String, List<LedgerPosting>> postingsByAccount = new HashMap<>();
        for (JournalEntry entry : allEntries) {
            for (LedgerPosting p : entry.getPostings()) {
                postingsByAccount.computeIfAbsent(p.getAccountId(), k -> new ArrayList<>()).add(p);
            }
        }

        List<LedgerDto.WalletReconciliationItem> walletBreakdowns = new ArrayList<>();
        int mismatched = 0;
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        for (LedgerAccount acc : allAccounts) {
            long sumDebit = 0;
            long sumCredit = 0;
            for (LedgerPosting p : postingsByAccount.getOrDefault(acc.getId(), List.of())) {
                if (p.getDirection() == PostingDirection.DEBIT) sumDebit += p.getAmount();
                else sumCredit += p.getAmount();
            }
            long calc = (acc.getAccountType() == AccountType.ASSET || acc.getAccountType() == AccountType.EXPENSE)
                    ? (sumDebit - sumCredit) : (sumCredit - sumDebit);
            long diff = Math.abs(acc.getBalance() - calc);
            boolean matched = (acc.getBalance() == calc);
            if (!matched) mismatched++;

            walletBreakdowns.add(new LedgerDto.WalletReconciliationItem(
                    acc.getAccountNumber(), acc.getAccountName(), acc.getAccountType().name(),
                    acc.getOwnerId(), acc.getBalance(), calc, matched, diff));

            recordSnapshotIfAbsent(today, acc);
        }

        boolean balanced = (totalDebitAll == totalCreditAll) && allDaysBalanced && (mismatched == 0);
        String msg = balanced
                ? "Sổ cái hoàn toàn cân đối: Tổng Nợ = Tổng Có, 100% số dư ví khớp chính xác."
                : "CẢNH BÁO: Sổ cái bị lệch Nợ/Có hoặc số dư ví không khớp bút toán!";
        boolean alertSent = !balanced && alertNotificationPort.sendAlert(
                "CẢNH BÁO TOÀN VẸN SỔ CÁI COZYNEST",
                String.format("Lệch: %,d VND, tài khoản lệch: %d", Math.abs(totalDebitAll - totalCreditAll), mismatched));

        saveReport(today, allEntries.size(), allAccounts.size() - mismatched, mismatched, balanced, msg);

        return new LedgerDto.ComprehensiveIntegrityReport(
                balanced, totalDebitAll, totalCreditAll, Math.abs(totalDebitAll - totalCreditAll),
                allAccounts.size(), mismatched, dailyBreakdowns, walletBreakdowns, alertSent, msg);
    }

    private void recordSnapshotIfAbsent(LocalDate today, LedgerAccount acc) {
        if (snapshotRepository == null) return;
        try {
            if (snapshotRepository.findByDateAndAccountId(today, acc.getId()).isEmpty()) {
                snapshotRepository.save(new LedgerDailySnapshot(
                        null, today, acc.getId(), acc.getAccountNumber(), acc.getBalance(), Instant.now()));
            }
        } catch (Exception ex) {
            log.warn("Cannot save daily ledger snapshot for {}: {}", acc.getAccountNumber(), ex.getMessage());
        }
    }

    private void saveReport(LocalDate today, int totalEntries, int matched, int mismatched, boolean balanced, String msg) {
        try {
            ReconciliationReport report = reconReportRepository.findByReconDate(today)
                    .map(r -> {
                        r.update(totalEntries, matched, mismatched, balanced ? "COMPLETED" : "MISMATCHED", msg);
                        return r;
                    })
                    .orElseGet(() -> ReconciliationReport.create(
                            today, totalEntries, matched, mismatched, balanced ? "COMPLETED" : "MISMATCHED", msg));
            reconReportRepository.save(report);
        } catch (Exception e) {
            log.warn("Cannot save reconciliation_reports: {}", e.getMessage());
        }
    }
}

