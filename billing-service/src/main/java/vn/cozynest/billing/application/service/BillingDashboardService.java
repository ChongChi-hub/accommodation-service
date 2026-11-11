package vn.cozynest.billing.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.DashboardStatsDto;
import vn.cozynest.billing.domain.model.Invoice;
import vn.cozynest.billing.domain.repository.InvoiceRepository;
import vn.cozynest.billing.domain.repository.JournalEntryRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class BillingDashboardService {
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
    public byte[] exportInvoicesCsvWithBom(String scopeRole, String userId) {
        List<Invoice> invoices = switch (scopeRole) {
            case "LANDLORD" -> invoiceRepository.findByLandlordId(userId);
            case "TENANT" -> invoiceRepository.findByTenantId(userId);
            default -> List.of();
        };
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("InvoiceCode,PeriodMonth,BaseRent,Electricity,Water,TotalAmount,PaidAmount,Status,Type\n");
        for (Invoice i : invoices) {
            sb.append(csvCell(i.getInvoiceCode())).append(',')
                    .append(i.getPeriodMonth()).append(',')
                    .append(i.getBaseRent()).append(',')
                    .append(i.getElectricityAmount()).append(',')
                    .append(i.getWaterAmount()).append(',')
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
