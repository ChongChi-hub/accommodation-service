package vn.cozynest.billing.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.DashboardStatsDto;
import vn.cozynest.billing.domain.model.Invoice;
import vn.cozynest.billing.domain.repository.InvoiceRepository;
import vn.cozynest.billing.domain.repository.JournalEntryRepository;

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

        for (Invoice inv : invoices) {
            totalRevenue += inv.getPaidAmount();
            if (!"PAID".equals(inv.getStatus()) && !"CANCELLED".equals(inv.getStatus())) {
                unpaidInvoicesTotal += (inv.getTotalAmount() - inv.getPaidAmount());
            } else if ("PAID".equals(inv.getStatus())) {
                paidCount++;
            }
        }

        return new DashboardStatsDto.LandlordFinancialStats(
                totalRevenue,
                availableBalance,
                escrowBalance,
                unpaidInvoicesTotal,
                invoices.size(),
                paidCount
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
                totalGmv,
                platformFeeRevenue,
                totalEscrowBalance,
                totalGatewayCash,
                totalTransactions
        );
    }
}

