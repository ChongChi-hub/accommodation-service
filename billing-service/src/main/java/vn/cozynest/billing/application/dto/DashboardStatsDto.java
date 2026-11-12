package vn.cozynest.billing.application.dto;

public class DashboardStatsDto {

    public record LandlordFinancialStats(
            long totalRevenue,
            long availableBalance,
            long pendingEscrowBalance,
            long unpaidInvoicesTotal,
            int totalInvoicesCount,
            int paidInvoicesCount,
            int overdueInvoicesCount
    ) {}

    public record TenantFinancialStats(
            long totalPaidAmount,
            long unpaidInvoicesTotal,
            long activeEscrowBalance,
            int overdueInvoicesCount,
            int totalInvoicesCount
    ) {}

    public record AdminFinancialStats(
            long totalGmv,
            long platformFeeRevenue,
            long totalEscrowBalance,
            long totalGatewayCash,
            int totalTransactionsCount
    ) {}
}
