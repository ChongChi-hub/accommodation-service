package vn.cozynest.billing.application.dto;

import java.util.List;

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

    public record MonthlyRevenuePoint(
            String period,
            long invoicedAmount,
            long collectedAmount,
            long overdueAmount
    ) {}

    public record LandlordAnalyticsResponse(
            LandlordFinancialStats summary,
            double occupancyRatePercent,
            List<MonthlyRevenuePoint> revenueSeries
    ) {}
}
