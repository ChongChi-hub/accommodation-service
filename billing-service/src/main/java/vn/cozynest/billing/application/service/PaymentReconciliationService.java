package vn.cozynest.billing.application.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.port.AlertNotificationPort;
import vn.cozynest.billing.domain.model.PaymentIntent;
import vn.cozynest.billing.domain.repository.PaymentIntentRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Automates gateway settlement reconciliation against internal gateway cash ledger (PAY-02).
 */
@Service
public class PaymentReconciliationService {

    private final PaymentIntentRepository paymentIntentRepository;
    private final LedgerService ledgerService;
    private final AlertNotificationPort alertPort;

    public PaymentReconciliationService(PaymentIntentRepository paymentIntentRepository,
                                        LedgerService ledgerService,
                                        AlertNotificationPort alertPort) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.ledgerService = ledgerService;
        this.alertPort = alertPort;
    }

    @Scheduled(cron = "${fintech.gateway.reconcile-cron:0 30 1 * * ?}")
    @Transactional(readOnly = true)
    public GatewayReconciliationReport reconcileGatewaySettlements() {
        List<PaymentIntent> intents = paymentIntentRepository.findAll();
        long expectedSettledTotal = 0L;
        List<String> anomalies = new ArrayList<>();

        for (PaymentIntent intent : intents) {
            String status = intent.getStatus();
            if ("PAID".equals(status) || "PARTIAL_PAID".equals(status) || "OVERPAID".equals(status)) {
                expectedSettledTotal += intent.getPaidAmount();
                if (!"PAID".equals(status)) {
                    anomalies.add(intent.getPaymentCode() + ":" + status + "(expected=" + intent.getAmount() + ",paid=" + intent.getPaidAmount() + ")");
                }
            } else if ("MISMATCH".equals(status) || ("EXPIRED".equals(status) && intent.getPaidAmount() > 0)) {
                anomalies.add(intent.getPaymentCode() + ":" + status + "(latePaid=" + intent.getPaidAmount() + ")");
            }
        }

        long ledgerGatewayBalance = ledgerService.getAccountBalance(LedgerService.ACC_GATEWAY);
        boolean matched = anomalies.isEmpty() && expectedSettledTotal == ledgerGatewayBalance;
        if (!matched) {
            alertPort.sendAlert(
                    "SLA ALERT [PAY-02]: Gateway Settlement Mismatch",
                    "SettledIntents=" + expectedSettledTotal + " VND, LedgerGateway=" + ledgerGatewayBalance + " VND, Anomalies=" + anomalies);
        }
        return new GatewayReconciliationReport(matched, expectedSettledTotal, ledgerGatewayBalance, anomalies);
    }

    public record GatewayReconciliationReport(
            boolean matched,
            long settledIntentsTotalVnd,
            long ledgerGatewayBalanceVnd,
            List<String> anomalies
    ) {}
}

