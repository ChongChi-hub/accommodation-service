package vn.cozynest.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import vn.cozynest.billing.application.dto.PaymentDto;
import vn.cozynest.billing.application.port.AlertNotificationPort;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.*;
import vn.cozynest.billing.infrastructure.gateway.SandboxVietQrGatewayAdapter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentHardeningTest {

    private static String hmac(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void handlesSecretRotationIpAllowlistUnderpaymentAndReconciliationAlert() throws Exception {
        PaymentIntentRepository intentRepo = mock(PaymentIntentRepository.class);
        InvoiceRepository invoiceRepo = mock(InvoiceRepository.class);
        LedgerAccountRepository accountRepo = mock(LedgerAccountRepository.class);
        JournalEntryRepository journalRepo = mock(JournalEntryRepository.class);
        AlertNotificationPort alertPort = mock(AlertNotificationPort.class);
        ReconciliationReportRepository reconRepo = mock(ReconciliationReportRepository.class);
        PaymentWebhookLogRepository logRepo = mock(PaymentWebhookLogRepository.class);

        LedgerAccount gateway = LedgerAccount.create("1010-GATEWAY", "Gateway", AccountType.ASSET, null);
        LedgerAccount escrow = LedgerAccount.create("2010-ESCROW", "Escrow", AccountType.LIABILITY, null);
        LedgerAccount revenue = LedgerAccount.create("4010-REVENUE", "Revenue", AccountType.REVENUE, null);

        when(accountRepo.findByAccountNumber("1010-GATEWAY")).thenReturn(Optional.of(gateway));
        when(accountRepo.findByAccountNumber("2010-ESCROW")).thenReturn(Optional.of(escrow));
        when(accountRepo.findByAccountNumber("4010-REVENUE")).thenReturn(Optional.of(revenue));
        when(accountRepo.findByAccountNumberForUpdate("1010-GATEWAY")).thenReturn(Optional.of(gateway));
        when(accountRepo.findByAccountNumberForUpdate("2010-ESCROW")).thenReturn(Optional.of(escrow));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(journalRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LedgerService ledgerService = new LedgerService(accountRepo, journalRepo, alertPort, reconRepo);
        var gatewayAdapter = new SandboxVietQrGatewayAdapter(
                "https://img.vietqr.io/image", "970422", "SANDBOX99", "COZYNEST%20SANDBOX", false);

        PaymentService paymentService = new PaymentService(
                intentRepo, invoiceRepo, ledgerService, new ObjectMapper(), gatewayAdapter, logRepo,
                "new-secret", "old-secret", true, "10.0.0.5");

        PaymentIntent intent = PaymentIntent.create("PAY-100", "tenant-1", "DEPOSIT", "cnt-1", 2_000_000L, "qr", 15);
        when(intentRepo.findByPaymentCode("PAY-100")).thenReturn(Optional.of(intent));
        when(intentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        long ts = Instant.now().getEpochSecond();
        String body = "{\"paymentCode\":\"PAY-100\",\"amount\":1500000,\"status\":\"SUCCESS\",\"timestamp\":" + ts + "}";
        String sigOld = hmac(body + "." + ts, "old-secret");

        // Unlisted IP rejected
        assertThatThrownBy(() -> paymentService.processWebhook(body, sigOld, ts, "192.168.1.99"))
                .isInstanceOf(SecurityException.class);

        // Listed IP with rotated previous secret accepted & marked PARTIAL_PAID
        boolean ok = paymentService.processWebhook(body, sigOld, ts, "10.0.0.5");
        assertThat(ok).isTrue();
        assertThat(intent.getStatus()).isEqualTo("PARTIAL_PAID");
        assertThat(gateway.getBalance()).isEqualTo(1_500_000L);

        // PAY-02 reconciliation flags PARTIAL_PAID anomaly and alerts
        when(intentRepo.findAll()).thenReturn(List.of(intent));
        PaymentReconciliationService reconService = new PaymentReconciliationService(intentRepo, ledgerService, alertPort);
        var report = reconService.reconcileGatewaySettlements();
        assertThat(report.matched()).isFalse();
        assertThat(report.anomalies()).anyMatch(s -> s.contains("PARTIAL_PAID"));
        verify(alertPort).sendAlert(any(), any());
    }

    @Test
    void enforcesGatewayKillSwitch() {
        var killedGateway = new SandboxVietQrGatewayAdapter(
                "https://img.vietqr.io/image", "970422", "SANDBOX99", "COZYNEST%20SANDBOX", true);
        PaymentService paymentService = new PaymentService(
                mock(PaymentIntentRepository.class), mock(InvoiceRepository.class), mock(LedgerService.class),
                new ObjectMapper(), killedGateway, mock(PaymentWebhookLogRepository.class),
                "secret", "", false, "127.0.0.1");

        assertThatThrownBy(() -> paymentService.createIntent("user-1", new PaymentDto.CreateIntentRequest("DEPOSIT", "cnt-1", 100_000L)))
                .isInstanceOf(IllegalStateException.class);
    }
}

