package vn.cozynest.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.cozynest.billing.application.dto.PaymentDto;
import vn.cozynest.billing.application.exception.ObligationAlreadyPaidException;
import vn.cozynest.billing.application.exception.WebhookSecurityException;
import vn.cozynest.billing.application.port.BillingContractPort;
import vn.cozynest.billing.application.port.PaymentGatewayPort;
import vn.cozynest.billing.domain.model.EscrowHold;
import vn.cozynest.billing.domain.model.PaymentIntent;
import vn.cozynest.billing.domain.repository.*;
import vn.cozynest.billing.infrastructure.gateway.SandboxVietQrGatewayAdapter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentSecurityHardeningPhase1Test {
    private PaymentIntentRepository intentRepo;
    private InvoiceRepository invoiceRepo;
    private LedgerService ledgerService;
    private PaymentGatewayPort gatewayPort;
    private PaymentWebhookLogRepository webhookLogRepo;
    private EscrowHoldRepository escrowHoldRepo;
    private BillingContractPort contractPort;
    private BillingEventOutboxRepository outboxRepo;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        intentRepo = mock(PaymentIntentRepository.class);
        invoiceRepo = mock(InvoiceRepository.class);
        ledgerService = mock(LedgerService.class);
        gatewayPort = new SandboxVietQrGatewayAdapter("https://img.vietqr.io/image", "970422", "SANDBOX99", "COZYNEST%20SANDBOX", false);
        webhookLogRepo = mock(PaymentWebhookLogRepository.class);
        escrowHoldRepo = mock(EscrowHoldRepository.class);
        contractPort = mock(BillingContractPort.class);
        outboxRepo = mock(BillingEventOutboxRepository.class);

        paymentService = new PaymentService(
                intentRepo, invoiceRepo, ledgerService, new ObjectMapper(), gatewayPort, webhookLogRepo,
                Optional.of(outboxRepo), Optional.empty(), Optional.of(contractPort),
                "test-secret", "", false, "127.0.0.1");
    }

    @Test
    void tenantCannotSimulateSandboxWebhook() {
        PaymentIntent intent = PaymentIntent.create("PAY-SEC-1", "tenant-1", "DEPOSIT", "cnt-1", 1_000_000L, "qr", 15);
        when(intentRepo.findByPaymentCode("PAY-SEC-1")).thenReturn(Optional.of(intent));

        assertThatThrownBy(() -> paymentService.simulateSandboxWebhook("PAY-SEC-1", 1_000_000L, "tenant-1", false))
                .isInstanceOf(WebhookSecurityException.class)
                .extracting(e -> ((WebhookSecurityException) e).getHttpStatus())
                .isEqualTo(403);
    }

    @Test
    void tenantSubmitIntentOnlyTransitionsToSubmittedWithoutPostingLedgerOrEscrow() {
        PaymentIntent intent = PaymentIntent.create("PAY-SEC-2", "tenant-1", "DEPOSIT", "cnt-2", 2_000_000L, "qr", 15);
        when(intentRepo.findByPaymentCode("PAY-SEC-2")).thenReturn(Optional.of(intent));
        when(intentRepo.save(any(PaymentIntent.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentDto.IntentResponse res = paymentService.submitIntent("PAY-SEC-2", "tenant-1", new PaymentDto.SubmitIntentRequest("Da chuyen qua app VCB", "https://img.cozynest/receipt.png"));

        assertThat(res.status()).isEqualTo("SUBMITTED");
        assertThat(intent.getStatus()).isEqualTo("SUBMITTED");
        verify(ledgerService, never()).postEntryIdempotent(any(), any(), any(), any(), any(), any());
        verify(outboxRepo, times(1)).save(any());
    }

    @Test
    void cannotCreateIntentForAlreadyPaidOrActiveContract() {
        BillingContractPort.ContractSnapshot activeSnap = new BillingContractPort.ContractSnapshot(
                "cnt-active", "r-1", "tenant-1", "landlord-1", "ACTIVE", 3_000_000L, 5, 3_000_000L, null);
        when(contractPort.getContractSnapshot(eq("cnt-active"), any())).thenReturn(Optional.of(activeSnap));

        assertThatThrownBy(() -> paymentService.createIntent("tenant-1", new PaymentDto.CreateIntentRequest("DEPOSIT", "cnt-active", 3_000_000L)))
                .isInstanceOf(ObligationAlreadyPaidException.class);
    }

    @Test
    void cannotCreateIntentWhenEscrowHoldAlreadyHeld() {
        BillingContractPort.ContractSnapshot snap = new BillingContractPort.ContractSnapshot(
                "cnt-held", "r-2", "tenant-1", "landlord-1", "AWAITING_DEPOSIT", 3_000_000L, 5, 3_000_000L, null);
        when(contractPort.getContractSnapshot(eq("cnt-held"), any())).thenReturn(Optional.of(snap));
        when(intentRepo.findByOrderIdAndOrderType("cnt-held", "DEPOSIT")).thenReturn(List.of(
                new PaymentIntent("id-1", "PAY-OLD", "tenant-1", "DEPOSIT", "cnt-held", 3_000_000L, 3_000_000L, "qr", "PAID", null, null, null)
        ));

        assertThatThrownBy(() -> paymentService.createIntent("tenant-1", new PaymentDto.CreateIntentRequest("DEPOSIT", "cnt-held", 3_000_000L)))
                .isInstanceOf(ObligationAlreadyPaidException.class);
    }

    @Test
    void landlordCanConfirmSubmittedPayment() {
        PaymentIntent intent = PaymentIntent.create("PAY-CONF-1", "tenant-1", "DEPOSIT", "cnt-conf", 2_000_000L, "qr", 15);
        intent.markSubmitted();
        when(intentRepo.findByPaymentCode("PAY-CONF-1")).thenReturn(Optional.of(intent));
        when(intentRepo.save(any(PaymentIntent.class))).thenAnswer(inv -> inv.getArgument(0));

        BillingContractPort.ContractSnapshot snap = new BillingContractPort.ContractSnapshot(
                "cnt-conf", "r-3", "tenant-1", "landlord-1", "AWAITING_DEPOSIT", 2_000_000L, 5, 2_000_000L, null);
        when(contractPort.getContractSnapshot(eq("cnt-conf"), any())).thenReturn(Optional.of(snap));

        PaymentDto.IntentResponse res = paymentService.confirmPaymentByLandlordOrAdmin(
                "PAY-CONF-1", new PaymentDto.ConfirmPaymentRequest(2_000_000L, "VCB123456789", "Da nhan du 2tr"), "landlord-1", false);

        assertThat(res.status()).isEqualTo("PAID");
        assertThat(intent.getStatus()).isEqualTo("PAID");
        verify(outboxRepo, atLeastOnce()).save(any());
    }

    @Test
    void unauthorizedLandlordCannotConfirmPayment() {
        PaymentIntent intent = PaymentIntent.create("PAY-CONF-2", "tenant-1", "DEPOSIT", "cnt-conf-2", 2_000_000L, "qr", 15);
        when(intentRepo.findByPaymentCode("PAY-CONF-2")).thenReturn(Optional.of(intent));

        BillingContractPort.ContractSnapshot snap = new BillingContractPort.ContractSnapshot(
                "cnt-conf-2", "r-4", "tenant-1", "landlord-original", "AWAITING_DEPOSIT", 2_000_000L, 5, 2_000_000L, null);
        when(contractPort.getContractSnapshot(eq("cnt-conf-2"), any())).thenReturn(Optional.of(snap));

        assertThatThrownBy(() -> paymentService.confirmPaymentByLandlordOrAdmin(
                "PAY-CONF-2", new PaymentDto.ConfirmPaymentRequest(2_000_000L, "VCB999", "Fake confirm"), "landlord-imposter", false))
                .isInstanceOf(WebhookSecurityException.class)
                .extracting(e -> ((WebhookSecurityException) e).getHttpStatus())
                .isEqualTo(403);
    }
}

