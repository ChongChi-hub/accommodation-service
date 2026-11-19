package vn.cozynest.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.cozynest.billing.application.dto.PaymentDto;
import vn.cozynest.billing.application.exception.ObligationAlreadyPaidException;
import vn.cozynest.billing.application.exception.WebhookSecurityException;
import vn.cozynest.billing.application.port.BillingContractPort;
import vn.cozynest.billing.application.port.PaymentGatewayPort;
import vn.cozynest.billing.domain.model.PaymentIntent;
import vn.cozynest.billing.domain.repository.*;
import vn.cozynest.billing.infrastructure.gateway.SandboxVietQrGatewayAdapter;

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
        gatewayPort = new SandboxVietQrGatewayAdapter("https://img.vietqr.io/image", "970436", "1042701900", "LE%20THANH%20CUONG", false);
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
        PaymentIntent intent = PaymentIntent.create("TOAMSEC00000001", "tenant-1", "DEPOSIT", "cnt-1", 1_000_000L, "qr", 15);
        when(intentRepo.findByPaymentCode("TOAMSEC00000001")).thenReturn(Optional.of(intent));

        assertThatThrownBy(() -> paymentService.simulateSandboxWebhook("TOAMSEC00000001", 1_000_000L, "tenant-1", false))
                .isInstanceOf(WebhookSecurityException.class)
                .extracting(e -> ((WebhookSecurityException) e).getHttpStatus())
                .isEqualTo(403);
    }

    @Test
    void tenantSubmitIntentOnlyMarksSubmittedAndNeverPostsLedgerOrCreditsEscrow() {
        PaymentIntent intent = PaymentIntent.create("TOAMSEC00000002", "tenant-1", "DEPOSIT", "cnt-2", 2_000_000L, "qr", 15);
        when(intentRepo.findByPaymentCode("TOAMSEC00000002")).thenReturn(Optional.of(intent));
        when(intentRepo.save(any(PaymentIntent.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentDto.IntentResponse res = paymentService.submitIntent("TOAMSEC00000002", "tenant-1",
                new PaymentDto.SubmitIntentRequest("BANK_TX:FT26111911111", "https://img.cozynest/receipt.png"));

        assertThat(res.status()).isEqualTo("SUBMITTED");
        assertThat(intent.getStatus()).isEqualTo("SUBMITTED");
        verify(ledgerService, never()).postEntryIdempotent(any(), any(), any(), any(), any(), any());
        verify(outboxRepo, times(1)).save(any());
    }

    @Test
    void tenantSubmitIntentRejectsUnverifiedTransferAndDoesNotPostEscrow() {
        PaymentIntent intent = PaymentIntent.create("TOAMSEC00000003", "tenant-1", "DEPOSIT", "cnt-unv", 2_000_000L, "qr", 15);
        when(intentRepo.findByPaymentCode("TOAMSEC00000003")).thenReturn(Optional.of(intent));

        assertThatThrownBy(() -> paymentService.submitIntent("TOAMSEC00000003", "tenant-1",
                new PaymentDto.SubmitIntentRequest("UNVERIFIED_TRANSFER", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Chưa ghi nhận được biến động tiền cọc về tài khoản Sàn");

        assertThat(intent.getStatus()).isEqualTo("PENDING");
        verify(ledgerService, never()).postEntryIdempotent(any(), any(), any(), any(), any(), any());
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
    void onlyAdminCanManuallyConfirmPlatformPaymentLandlordGets403() {
        PaymentIntent intent = PaymentIntent.create("TOAMCONF0000001", "tenant-1", "DEPOSIT", "cnt-conf", 2_000_000L, "qr", 15);
        intent.markSubmitted();
        when(intentRepo.findByPaymentCode("TOAMCONF0000001")).thenReturn(Optional.of(intent));
        when(intentRepo.save(any(PaymentIntent.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> paymentService.confirmPaymentByLandlordOrAdmin(
                "TOAMCONF0000001", new PaymentDto.ConfirmPaymentRequest(2_000_000L, "VCB123456789", "Landlord try"), "landlord-1", false))
                .isInstanceOf(WebhookSecurityException.class)
                .extracting(e -> ((WebhookSecurityException) e).getHttpStatus())
                .isEqualTo(403);

        PaymentDto.IntentResponse res = paymentService.confirmPaymentByLandlordOrAdmin(
                "TOAMCONF0000001", new PaymentDto.ConfirmPaymentRequest(2_000_000L, "VCB123456789", "Admin sao ke"), "admin-1", true);

        assertThat(res.status()).isEqualTo("PAID");
        assertThat(intent.getStatus()).isEqualTo("PAID");
        verify(outboxRepo, atLeastOnce()).save(any());
    }

    @Test
    void rejectsReusedBankTxCodeAndReusedReceiptImage() {
        EscrowService escrowService = new EscrowService(ledgerService, escrowHoldRepo, "SANDBOX_ONLY", false);
        when(escrowHoldRepo.existsByBankTxCode("FT261119001")).thenReturn(true);
        when(escrowHoldRepo.existsByReceiptHash("sha256-usedreceipt")).thenReturn(true);

        assertThatThrownBy(() -> escrowService.recordDepositPaidWithProof(
                "cnt-replay-1", "tenant-1", "landlord-1", 5_000L, 72, "TOAM1", "FT261119001", "sha256-new", "data:image/png;base64,abc"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mã giao dịch ngân hàng [FT261119001] đã được sử dụng");

        assertThatThrownBy(() -> escrowService.recordDepositPaidWithProof(
                "cnt-replay-2", "tenant-1", "landlord-1", 5_000L, 72, "TOAM2", "FT261119002", "sha256-usedreceipt", "data:image/png;base64,abc"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ảnh biên lai chuyển khoản này đã được sử dụng");
    }
}
