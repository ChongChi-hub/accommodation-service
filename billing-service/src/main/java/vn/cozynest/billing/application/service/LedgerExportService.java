package vn.cozynest.billing.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.JournalEntryRepository;
import vn.cozynest.billing.domain.repository.LedgerAccountRepository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LedgerExportService {

    private static final DateTimeFormatter CSV_DTF =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final LedgerAccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;

    public LedgerExportService(LedgerAccountRepository accountRepository,
                               JournalEntryRepository journalEntryRepository) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    @Transactional(readOnly = true)
    public String exportLedgerReportCsv() {
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("Mã tài khoản,Tên tài khoản,Loại tài khoản,Số dư (VND),Đóng băng\n");
        for (LedgerAccount acc : accountRepository.findAll()) {
            sb.append('"').append(escapeCsv(acc.getAccountNumber())).append("\",")
                    .append('"').append(escapeCsv(acc.getAccountName())).append("\",")
                    .append('"').append(acc.getAccountType()).append("\",")
                    .append(acc.getBalance()).append(",")
                    .append(acc.isFrozen() ? "Đã khóa" : "Hoạt động").append("\n");
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String exportGeneralLedgerCsv(Instant from, Instant to) {
        List<JournalEntry> entries = journalEntryRepository.findAll();
        Map<String, LedgerAccount> accountMap = accountRepository.findAll().stream()
                .collect(Collectors.toMap(LedgerAccount::getId, a -> a));

        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("Mã bút toán,Ngày giờ hạch toán (UTC),Mã tài khoản,Tên tài khoản,Diễn giải,Loại nghiệp vụ,Mã tham chiếu,Nợ (VND),Có (VND),Người tạo\n");

        for (JournalEntry entry : entries) {
            Instant created = entry.getCreatedAt();
            if ((from != null && created.isBefore(from)) || (to != null && created.isAfter(to))) continue;
            for (LedgerPosting p : entry.getPostings()) {
                LedgerAccount acc = accountMap.get(p.getAccountId());
                String accNum = acc != null ? acc.getAccountNumber() : p.getAccountId();
                String accName = acc != null ? acc.getAccountName() : "N/A";
                long debit = p.getDirection() == PostingDirection.DEBIT ? p.getAmount() : 0L;
                long credit = p.getDirection() == PostingDirection.CREDIT ? p.getAmount() : 0L;

                sb.append('"').append(escapeCsv(entry.getEntryNumber())).append("\",")
                        .append('"').append(CSV_DTF.format(created)).append("\",")
                        .append('"').append(escapeCsv(accNum)).append("\",")
                        .append('"').append(escapeCsv(accName)).append("\",")
                        .append('"').append(escapeCsv(entry.getDescription())).append("\",")
                        .append('"').append(escapeCsv(entry.getReferenceType())).append("\",")
                        .append('"').append(escapeCsv(entry.getReferenceId())).append("\",")
                        .append(debit).append(",").append(credit).append(",")
                        .append('"').append(escapeCsv(entry.getCreatedBy())).append("\"\n");
            }
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String exportTrialBalanceCsv(Instant from, Instant to) {
        List<JournalEntry> entries = journalEntryRepository.findAll();
        List<LedgerAccount> accounts = accountRepository.findAll();

        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("Mã tài khoản,Tên tài khoản,Loại tài khoản,Dư Nợ đầu kỳ (VND),Dư Có đầu kỳ (VND),Phát sinh Nợ (VND),Phát sinh Có (VND),Dư Nợ cuối kỳ (VND),Dư Có cuối kỳ (VND)\n");
        long totalPeriodDebit = 0, totalPeriodCredit = 0, totalClosingDebit = 0, totalClosingCredit = 0;

        for (LedgerAccount acc : accounts) {
            long openD = 0, openC = 0, periodD = 0, periodC = 0;
            for (JournalEntry entry : entries) {
                Instant created = entry.getCreatedAt();
                for (LedgerPosting p : entry.getPostings()) {
                    if (!p.getAccountId().equals(acc.getId())) continue;
                    boolean before = from != null && created.isBefore(from);
                    boolean inPeriod = (from == null || !created.isBefore(from)) && (to == null || !created.isAfter(to));
                    if (before) {
                        if (p.getDirection() == PostingDirection.DEBIT) openD += p.getAmount(); else openC += p.getAmount();
                    } else if (inPeriod) {
                        if (p.getDirection() == PostingDirection.DEBIT) periodD += p.getAmount(); else periodC += p.getAmount();
                    }
                }
            }
            boolean normalDebit = acc.getAccountType() == AccountType.ASSET || acc.getAccountType() == AccountType.EXPENSE;
            long[] openNet = computeNet(normalDebit, openD, openC);
            long[] closeNet = computeNet(normalDebit, openD + periodD, openC + periodC);

            totalPeriodDebit += periodD;
            totalPeriodCredit += periodC;
            totalClosingDebit += closeNet[0];
            totalClosingCredit += closeNet[1];

            sb.append('"').append(escapeCsv(acc.getAccountNumber())).append("\",")
                    .append('"').append(escapeCsv(acc.getAccountName())).append("\",")
                    .append('"').append(acc.getAccountType()).append("\",")
                    .append(openNet[0]).append(",").append(openNet[1]).append(",")
                    .append(periodD).append(",").append(periodC).append(",")
                    .append(closeNet[0]).append(",").append(closeNet[1]).append("\n");
        }
        sb.append("\"TỔNG CỘNG\",\"Cân đối toàn hệ thống\",\"TOTAL\",0,0,")
                .append(totalPeriodDebit).append(",").append(totalPeriodCredit).append(",")
                .append(totalClosingDebit).append(",").append(totalClosingCredit).append("\n");
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String exportAccountStatementCsv(String accountNumber, Instant from, Instant to) {
        LedgerAccount acc = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản: " + accountNumber));
        List<JournalEntry> entries = new ArrayList<>(journalEntryRepository.findAll());
        entries.sort(Comparator.comparing(JournalEntry::getCreatedAt));

        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("Sao kê tài khoản: ").append(escapeCsv(acc.getAccountNumber())).append(" - ").append(escapeCsv(acc.getAccountName())).append("\n");
        sb.append("Ngày giờ (UTC),Mã bút toán,Diễn giải,Nợ (VND),Có (VND),Số dư lũy kế (VND),Mã tham chiếu\n");

        long running = 0;
        boolean normalDebit = acc.getAccountType() == AccountType.ASSET || acc.getAccountType() == AccountType.EXPENSE;
        for (JournalEntry entry : entries) {
            Instant created = entry.getCreatedAt();
            for (LedgerPosting p : entry.getPostings()) {
                if (!p.getAccountId().equals(acc.getId())) continue;
                long d = p.getDirection() == PostingDirection.DEBIT ? p.getAmount() : 0L;
                long c = p.getDirection() == PostingDirection.CREDIT ? p.getAmount() : 0L;
                running += normalDebit ? (d - c) : (c - d);
                if ((from != null && created.isBefore(from)) || (to != null && created.isAfter(to))) continue;
                sb.append('"').append(CSV_DTF.format(created)).append("\",")
                        .append('"').append(escapeCsv(entry.getEntryNumber())).append("\",")
                        .append('"').append(escapeCsv(entry.getDescription())).append("\",")
                        .append(d).append(",").append(c).append(",").append(running).append(",")
                        .append('"').append(escapeCsv(entry.getReferenceId())).append("\"\n");
            }
        }
        return sb.toString();
    }

    private long[] computeNet(boolean normalDebit, long debit, long credit) {
        long net = normalDebit ? (debit - credit) : (credit - debit);
        return normalDebit
                ? (net >= 0 ? new long[]{net, 0L} : new long[]{0L, -net})
                : (net >= 0 ? new long[]{0L, net} : new long[]{-net, 0L});
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        String sanitized = val;
        if (!sanitized.isEmpty()) {
            char first = sanitized.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@') {
                sanitized = "'" + sanitized;
            }
        }
        return sanitized.replace("\"", "\"\"");
    }
}

