package vn.cozynest.billing.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.DashboardStatsDto;
import vn.cozynest.billing.domain.model.Invoice;
import vn.cozynest.billing.domain.repository.InvoiceRepository;
import vn.cozynest.billing.domain.repository.JournalEntryRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BillingDashboardService {
    private static final Logger log = LoggerFactory.getLogger(BillingDashboardService.class);
    private static final int MAX_EXPORT_ROWS = 5000;
    private static final long MAX_EXPORT_DAYS = 366;

    private final LedgerService ledgerService;
    private final InvoiceRepository invoiceRepository;
    private final JournalEntryRepository journalEntryRepository;

    public BillingDashboardService(LedgerService ledgerService,
                                   InvoiceRepository invoiceRepository,
                                   JournalEntryRepository journalEntryRepository) {
        this.ledgerService = ledgerService;
        this.invoiceRepository = invoiceRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto.LandlordFinancialStats getLandlordFinancialStats(String landlordId) {
        String walletAcc = ledgerService.getWalletAccountNumber(landlordId);
        long availableBalance = ledgerService.getAccountBalance(walletAcc);
        long escrowBalance = ledgerService.getAccountBalance(LedgerService.ACC_ESCROW);

        List<Invoice> invoices = invoiceRepository.findByLandlordId(landlordId);
        long totalRevenue = 0;
        long unpaidInvoicesTotal = 0;
        int paidCount = 0;
        int overdueCount = 0;

        for (Invoice inv : invoices) {
            totalRevenue += inv.getPaidAmount();
            if ("PAID".equals(inv.getStatus())) {
                paidCount++;
            } else if (!"CANCELLED".equals(inv.getStatus())) {
                unpaidInvoicesTotal += Math.max(0, inv.getTotalAmount() - inv.getPaidAmount());
                if ("OVERDUE".equals(inv.getStatus())) {
                    overdueCount++;
                }
            }
        }
        return new DashboardStatsDto.LandlordFinancialStats(
                totalRevenue, availableBalance, escrowBalance, unpaidInvoicesTotal,
                invoices.size(), paidCount, overdueCount
        );
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto.TenantFinancialStats getTenantFinancialStats(String tenantId) {
        List<Invoice> invoices = invoiceRepository.findByTenantId(tenantId);
        long totalPaid = 0;
        long unpaidTotal = 0;
        int overdueCount = 0;
        for (Invoice inv : invoices) {
            totalPaid += inv.getPaidAmount();
            if (!"PAID".equals(inv.getStatus()) && !"CANCELLED".equals(inv.getStatus())) {
                unpaidTotal += Math.max(0, inv.getTotalAmount() - inv.getPaidAmount());
                if ("OVERDUE".equals(inv.getStatus())) {
                    overdueCount++;
                }
            }
        }
        long escrowBalance = ledgerService.getAccountBalance(LedgerService.ACC_ESCROW);
        return new DashboardStatsDto.TenantFinancialStats(
                totalPaid, unpaidTotal, escrowBalance, overdueCount, invoices.size()
        );
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto.AdminFinancialStats getAdminFinancialStats() {
        long platformFeeRevenue = ledgerService.getAccountBalance(LedgerService.ACC_REVENUE);
        long totalEscrowBalance = ledgerService.getAccountBalance(LedgerService.ACC_ESCROW);
        long totalGatewayCash = ledgerService.getAccountBalance(LedgerService.ACC_GATEWAY);
        int totalTransactions = journalEntryRepository.findAll().size();
        long totalGmv = totalGatewayCash + platformFeeRevenue;

        return new DashboardStatsDto.AdminFinancialStats(
                totalGmv, platformFeeRevenue, totalEscrowBalance, totalGatewayCash, totalTransactions
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "billingDashboardStats", key = "#landlordId + ':' + #occupiedRooms + ':' + #totalRooms")
    public DashboardStatsDto.LandlordAnalyticsResponse getLandlordAnalytics(
            String landlordId, int occupiedRooms, int totalRooms) {
        DashboardStatsDto.LandlordFinancialStats summary = getLandlordFinancialStats(landlordId);
        double occupancyRate = totalRooms > 0
                ? Math.min(100.0, Math.round((occupiedRooms * 1000.0) / totalRooms) / 10.0)
                : 0.0;
        List<Invoice> invoices = invoiceRepository.findByLandlordId(landlordId);
        Map<String, long[]> buckets = new LinkedHashMap<>();
        invoices.stream()
                .filter(i -> i.getPeriodMonth() != null && !"CANCELLED".equals(i.getStatus()))
                .sorted(Comparator.comparing(Invoice::getPeriodMonth))
                .forEach(inv -> {
                    String key = inv.getPeriodMonth().getYear() + "-" + String.format("%02d", inv.getPeriodMonth().getMonthValue());
                    long[] acc = buckets.computeIfAbsent(key, k -> new long[3]);
                    acc[0] += Math.max(0L, inv.getTotalAmount());
                    acc[1] += inv.getPaidAmount();
                    if ("OVERDUE".equals(inv.getStatus())) {
                        acc[2] += Math.max(0L, inv.getTotalAmount() - inv.getPaidAmount());
                    }
                });
        List<DashboardStatsDto.MonthlyRevenuePoint> series = buckets.entrySet().stream()
                .map(e -> new DashboardStatsDto.MonthlyRevenuePoint(
                        e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .toList();
        return new DashboardStatsDto.LandlordAnalyticsResponse(summary, occupancyRate, series);
    }

    @Transactional(readOnly = true)
    public byte[] exportInvoicesCsvWithBom(String scopeRole, String userId) {
        return exportInvoicesCsvWithBom(scopeRole, userId, null, null);
    }

    @Transactional(readOnly = true)
    public byte[] exportInvoicesCsvWithBom(String scopeRole, String userId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null) {
            if (toDate.isBefore(fromDate)) {
                throw new IllegalArgumentException("Khoang ngay xuat bao cao khong hop le");
            }
            if (ChronoUnit.DAYS.between(fromDate, toDate) > MAX_EXPORT_DAYS) {
                throw new IllegalArgumentException("Khoang thoi gian xuat CSV toi da 366 ngay");
            }
        }
        List<Invoice> invoices = (switch (scopeRole) {
            case "LANDLORD" -> invoiceRepository.findByLandlordId(userId);
            case "TENANT" -> invoiceRepository.findByTenantId(userId);
            default -> List.<Invoice>of();
        }).stream()
                .filter(i -> fromDate == null || (i.getPeriodMonth() != null && !i.getPeriodMonth().isBefore(fromDate)))
                .filter(i -> toDate == null || (i.getPeriodMonth() != null && !i.getPeriodMonth().isAfter(toDate)))
                .limit(MAX_EXPORT_ROWS)
                .toList();

        log.info("Audit CSV export: role={} userId={} rows={} from={} to={}",
                scopeRole, userId, invoices.size(), fromDate, toDate);
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("InvoiceCode,PeriodMonth,BaseRent,Electricity,Water,LateFee,CreditApplied,TotalAmount,PaidAmount,Status,Type\n");
        for (Invoice i : invoices) {
            sb.append(csvCell(i.getInvoiceCode())).append(',')
                    .append(i.getPeriodMonth()).append(',')
                    .append(i.getBaseRent()).append(',')
                    .append(i.getElectricityAmount()).append(',')
                    .append(i.getWaterAmount()).append(',')
                    .append(i.getLateFeeAmount()).append(',')
                    .append(i.getCreditAppliedAmount()).append(',')
                    .append(i.getTotalAmount()).append(',')
                    .append(i.getPaidAmount()).append(',')
                    .append(csvCell(i.getStatus())).append(',')
                    .append(csvCell(i.getInvoiceType())).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvCell(String raw) {
        if (raw == null) return "";
        String v = raw.replace("\"", "\"\"");
        if (!v.isEmpty() && ("=+-@\t\r".indexOf(v.charAt(0)) >= 0)) {
            v = "'" + v;
        }
        return "\"" + v + "\"";
    }
}
