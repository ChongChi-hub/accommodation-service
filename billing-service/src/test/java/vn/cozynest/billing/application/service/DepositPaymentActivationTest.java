package vn.cozynest.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import vn.cozynest.billing.application.dto.PaymentDto;
import vn.cozynest.billing.application.exception.WebhookSecurityException;
import vn.cozynest.billing.application.port.AlertNotificationPort;
import vn.cozynest.billing.application.port.BillingContractPort;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.*;
import vn.cozynest.billing.infrastructure.gateway.SandboxVietQrGatewayAdapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DepositPaymentActivationTest {

    @Test
    void computesDepositServerSideEnforcesSinglePendingAndActivatesContractOnPaid() {
        PaymentIntentRepository intentRepo = mock(PaymentIntentRepository.class);
        InvoiceRepository invoiceRepo = mock(InvoiceRepository.class);
        LedgerAccountRepository accountRepo = mock(LedgerAccountRepository.class);
        JournalEntryRepository journalRepo = mock(JournalEntryRepository.class);
        PaymentWebhookLogRepository logRepo = mock(PaymentWebhookLogRepository.class);
        BillingContractPort contractPort = mock(BillingContractPort.class);

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
        when(intentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LedgerService ledgerService = new LedgerService(
                accountRepo, journalRepo, mock(AlertNotificationPort.class), mock(ReconciliationReportRepository.class)
        );
        var gatewayAdapter = new SandboxVietQrGatewayAdapter(
                "https://img.vietqr.io/image", "970422", "SANDBOX99", "COZYNEST%20SANDBOX", false
        );

        PaymentService service = new PaymentService(
                intentRepo, invoiceRepo, ledgerService, new ObjectMapper(), gatewayAdapter, logRepo,
                Optional.empty(), Optional.empty(), Optional.of(contractPort),
                "test-secret", "", false, "127.0.0.1"
        );

        var awaitingSnap = new BillingContractPort.ContractSnapshot(
                "ct-new-1", "206", "u-tenant-01", "u-landlord-01",
                "AWAITING_DEPOSIT", 4_500_000L, 5, 4_500_000L, null
        );
        when(contractPort.getContractSnapshot(eq("ct-new-1"), any())).thenReturn(Optional.of(awaitingSnap));
        when(intentRepo.findByOrderIdAndOrderTypeAndStatus("ct-new-1", "DEPOSIT", "PENDING")).thenReturn(List.of());

        // IDOR: other tenant gets 404
        assertThatThrownBy(() -> service.createIntent("u-tenant-02", new PaymentDto.CreateIntentRequest("DEPOSIT", "ct-new-1", 1000L)))
                .isInstanceOf(WebhookSecurityException.class)
                .extracting(e -> ((WebhookSecurityException) e).getHttpStatus())
                .isEqualTo(404);

        // Server computes 4,500,000 even if client tries to send 1,000
        PaymentDto.IntentResponse created = service.createIntent(
                "u-tenant-01", new PaymentDto.CreateIntentRequest("DEPOSIT", "ct-new-1", 1000L)
        );
        assertThat(created.amount()).isEqualTo(4_500_000L);

        // Second pending deposit intent on same contract is rejected with 409 (IllegalStateException)
        PaymentIntent activePending = PaymentIntent.create(
                created.paymentCode(), "u-tenant-01", "DEPOSIT", "ct-new-1", 4_500_000L, created.qrUrl(), 15
        );
        when(intentRepo.findByOrderIdAndOrderTypeAndStatus("ct-new-1", "DEPOSIT", "PENDING"))
                .thenReturn(List.of(activePending));
        assertThatThrownBy(() -> service.createIntent("u-tenant-01", new PaymentDto.CreateIntentRequest("DEPOSIT", "ct-new-1", 0L)))
                .isInstanceOf(IllegalStateException.class);

        // Simulate sandbox webhook -> PAID -> triggers activateContractAfterDeposit
        when(intentRepo.findByPaymentCode(created.paymentCode())).thenReturn(Optional.of(activePending));
        when(contractPort.activateContractAfterDeposit("ct-new-1", created.paymentCode(), 4_500_000L)).thenReturn(true);

        PaymentDto.IntentResponse paid = service.simulateSandboxWebhook(created.paymentCode(), 4_500_000L, "u-tenant-01", false);
        assertThat(paid.status()).isEqualTo("PAID");
        verify(contractPort, times(1)).activateContractAfterDeposit("ct-new-1", created.paymentCode(), 4_500_000L);
    }

    @Test
    void rejectsDepositOnPendingSignContract() {
        BillingContractPort contractPort = mock(BillingContractPort.class);
        when(contractPort.getContractSnapshot(eq("ct-unsigned"), any())).thenReturn(Optional.of(
                new BillingContractPort.ContractSnapshot(
                        "ct-unsigned", "206", "u-tenant-01", "u-landlord-01",
                        "PENDING_SIGN", 4_000_000L, 5, 4_000_000L, null
                )
        ));
        var gatewayAdapter = new SandboxVietQrGatewayAdapter(
                "https://img.vietqr.io/image", "970422", "SANDBOX99", "COZYNEST%20SANDBOX", false
        );
        PaymentService service = new PaymentService(
                mock(PaymentIntentRepository.class), mock(InvoiceRepository.class), mock(LedgerService.class),
                new ObjectMapper(), gatewayAdapter, mock(PaymentWebhookLogRepository.class),
                Optional.empty(), Optional.empty(), Optional.of(contractPort),
                "test-secret", "", false, "127.0.0.1"
        );

        assertThatThrownBy(() -> service.createIntent("u-tenant-01", new PaymentDto.CreateIntentRequest("DEPOSIT", "ct-unsigned", 0L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING_SIGN");
    }
}

