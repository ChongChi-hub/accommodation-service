package vn.cozynest.billing.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.port.AlertNotificationPort;
import vn.cozynest.billing.domain.model.PaymentIntent;
import vn.cozynest.billing.domain.model.PayoutRequest;
import vn.cozynest.billing.domain.model.ReconciliationReport;
import vn.cozynest.billing.domain.model.RefundRequest;
import vn.cozynest.billing.domain.repository.PaymentIntentRepository;
import vn.cozynest.billing.domain.repository.PayoutRequestRepository;
import vn.cozynest.billing.domain.repository.ReconciliationReportRepository;
import vn.cozynest.billing.domain.repository.RefundRequestRepository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Automates gateway settlement reconciliation against internal gateway cash ledger (PAY-02),
 * persisting daily reports with SLA escalation for discrepancies.
 */
@Service
public class PaymentReconciliationService {

    private final PaymentIntentRepository paymentIntentRepository;
    private final LedgerService ledgerService;
    private final AlertNotificationPort alertPort;
    private final ReconciliationReportRepository reportRepository;
    private final RefundRequestRepository refundRepository;
    private final PayoutRequestRepository payoutRepository;

    public PaymentReconciliationService(PaymentIntentRepository paymentIntentRepository,
                                        LedgerService ledgerService,
                                        AlertNotificationPort alertPort) {
        this(paymentIntentRepository, ledgerService, alertPort,
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Autowired
    public PaymentReconciliationService(PaymentIntentRepository paymentIntentRepository,
                                        LedgerService ledgerService,
                                        AlertNotificationPort alertPort,
                                        Optional<ReconciliationReportRepository> reportRepository,
                                        Optional<RefundRequestRepository> refundRepository,
                                        Optional<PayoutRequestRepository> payoutRepository) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.ledgerService = ledgerService;
        this.alertPort = alertPort;
        this.reportRepository = reportRepository != null ? reportRepository.orElse(null) : null;
        this.refundRepository = refundRepository != null ? refundRepository.orElse(null) : null;
        this.payoutRepository = payoutRepository != null ? payoutRepository.orElse(null) : null;
    }

    @Scheduled(cron = "${fintech.gateway.reconcile-cron:0 30 1 * * ?}")
    @Transactional
    public GatewayReconciliationReport reconcileGatewaySettlements() {
        List<PaymentIntent> intents = paymentIntentRepository.findAll();
        long expectedSettledTotal = 0L;
        int matchedCount = 0;
        List<String> anomalies = new ArrayList<>();

        for (PaymentIntent intent : intents) {
            String status = intent.getStatus();
            if ("PAID".equals(status) || "PARTIAL_PAID".equals(status) || "OVERPAID".equals(status)) {
                expectedSettledTotal += intent.getPaidAmount();
                if ("PAID".equals(status)) {
                    matchedCount++;
                } else {
                    anomalies.add(intent.getPaymentCode() + ":" + status
                            + "(expected=" + intent.getAmount() + ",paid=" + intent.getPaidAmount() + ")");
                }
            } else if ("MISMATCH".equals(status) || ("EXPIRED".equals(status) && intent.getPaidAmount() > 0)) {
                anomalies.add(intent.getPaymentCode() + ":" + status + "(latePaid=" + intent.getPaidAmount() + ")");
            }
        }

        long completedRefunds = 0L;
        int totalOutflowTx = 0;
        if (refundRepository != null) {
            for (RefundRequest r : refundRepository.findAll()) {
                if ("COMPLETED".equals(r.getStatus())) {
                    completedRefunds += r.getAmount();
                    totalOutflowTx++;
                    matchedCount++;
                } else if ("FAILED".equals(r.getStatus())) {
                    anomalies.add(r.getRefundCode() + ":REFUND_FAILED(" + r.getFailureReason() + ")");
                }
            }
        }
        long completedPayouts = 0L;
        if (payoutRepository != null) {
            for (PayoutRequest p : payoutRepository.findAll()) {
                if ("COMPLETED".equals(p.getStatus())) {
                    completedPayouts += p.getAmount();
                    totalOutflowTx++;
                    matchedCount++;
                } else if ("FAILED".equals(p.getStatus())) {
                    anomalies.add(p.getPayoutCode() + ":PAYOUT_FAILED(" + p.getFailureReason() + ")");
                }
            }
        }

        long expectedNetGatewayCash = expectedSettledTotal - completedRefunds - completedPayouts;
        long ledgerGatewayBalance = ledgerService.getAccountBalance(LedgerService.ACC_GATEWAY);
        int missingInternalTx = expectedNetGatewayCash < ledgerGatewayBalance ? 1 : 0;
        int missingProviderTx = expectedNetGatewayCash > ledgerGatewayBalance ? 1 : 0;
        boolean matched = anomalies.isEmpty() && expectedNetGatewayCash == ledgerGatewayBalance;

        if (!matched) {
            alertPort.sendAlert(
                    "SLA ALERT [PAY-02]: Gateway Settlement Mismatch",
                    "NetGatewayExpected=" + expectedNetGatewayCash + " VND, LedgerGateway=" + ledgerGatewayBalance
                            + " VND, Anomalies=" + anomalies);
        }

        if (reportRepository != null) {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            int totalInternalTx = intents.size() + totalOutflowTx;
            int finalMatchedCount = matchedCount;
            int mismatchCount = anomalies.size() + missingInternalTx + missingProviderTx;
            String reportStatus = matched ? "COMPLETED" : "MISMATCH_ALERT";
            String summary = "Settled=" + expectedSettledTotal + ", Refunds=" + completedRefunds
                    + ", Payouts=" + completedPayouts + ", Ledger=" + ledgerGatewayBalance
                    + (anomalies.isEmpty() ? "" : ", Anomalies=" + String.join("; ", anomalies));

            ReconciliationReport report = reportRepository.findByReconDate(today)
                    .map(existing -> {
                        existing.update(totalInternalTx, finalMatchedCount, mismatchCount, reportStatus, summary);
                        existing.updateGatewayCounts(totalInternalTx, missingInternalTx, missingProviderTx);
                        return existing;
                    })
                    .orElseGet(() -> {
                        ReconciliationReport created = ReconciliationReport.create(
                                today, totalInternalTx, finalMatchedCount, mismatchCount, reportStatus, summary);
                        created.updateGatewayCounts(totalInternalTx, missingInternalTx, missingProviderTx);
                        return created;
                    });
            reportRepository.save(report);
        }

        return new GatewayReconciliationReport(matched, expectedNetGatewayCash, ledgerGatewayBalance, anomalies);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationReport> getRecentReports() {
        if (reportRepository == null) {
            return List.of();
        }
        return reportRepository.findRecentReports(30);
    }

    @Transactional
    public ReconciliationReport resolveReport(String reportId, String adminId, String notes) {
        if (reportRepository == null) {
            throw new IllegalStateException("Reconciliation report repository unavailable");
        }
        ReconciliationReport report = reportRepository.findRecentReports(90).stream()
                .filter(r -> r.getId().equals(reportId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay bao cao doi soat: " + reportId));
        report.assignTo(adminId);
        report.markResolved(adminId, notes);
        return reportRepository.save(report);
    }

    public record GatewayReconciliationReport(
            boolean matched,
            long settledIntentsTotalVnd,
            long ledgerGatewayBalanceVnd,
            List<String> anomalies
    ) {}
}
