package vn.cozynest.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import vn.cozynest.billing.application.dto.PayoutDto;
import vn.cozynest.billing.application.dto.RefundDto;
import vn.cozynest.billing.application.exception.WebhookSecurityException;
import vn.cozynest.billing.application.port.AlertNotificationPort;
import vn.cozynest.billing.application.port.BillingIdentityVerificationPort;
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

class Wave2FintechSecurityAndAuditTest {

    private static String hmac(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void enforces401InvalidSig403IpIdempotencyAndPersistsDailyReconciliation() throws Exception {
        PaymentIntentRepository intentRepo = mock(PaymentIntentRepository.class);
        InvoiceRepository invoiceRepo = mock(InvoiceRepository.class);
        LedgerAccountRepository accountRepo = mock(LedgerAccountRepository.class);
        JournalEntryRepository journalRepo = mock(JournalEntryRepository.class);
        AlertNotificationPort alertPort = mock(AlertNotificationPort.class);
        ReconciliationReportRepository reportRepo = mock(ReconciliationReportRepository.class);
        BillingEventOutboxRepository outboxRepo = mock(BillingEventOutboxRepository.class);

        LedgerAccount gateway = LedgerAccount.create("1010-GATEWAY", "Gateway", AccountType.ASSET, null);
        LedgerAccount escrow = LedgerAccount.create("2010-ESCROW", "Escrow", AccountType.LIABILITY, null);
        when(accountRepo.findByAccountNumber("1010-GATEWAY")).thenReturn(Optional.of(gateway));
        when(accountRepo.findByAccountNumber("2010-ESCROW")).thenReturn(Optional.of(escrow));
        when(accountRepo.findByAccountNumberForUpdate("1010-GATEWAY")).thenReturn(Optional.of(gateway));
        when(accountRepo.findByAccountNumberForUpdate("2010-ESCROW")).thenReturn(Optional.of(escrow));
        when(accountRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(journalRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        LedgerService ledger = new LedgerService(accountRepo, journalRepo, alertPort, reportRepo);
        PaymentService service = new PaymentService(
                intentRepo, invoiceRepo, ledger, new ObjectMapper(),
                new SandboxVietQrGatewayAdapter("https://img.vietqr.io/image", "970422", "SANDBOX", "COZYNEST", false),
                mock(PaymentWebhookLogRepository.class), Optional.of(outboxRepo),
                "secret-a", "", true, "127.0.0.1"
        );

        PaymentIntent intent = PaymentIntent.create("PAY-200", "t1", "DEPOSIT", "cnt-2", 1_000_000L, "qr", 15);
        when(intentRepo.findByPaymentCode("PAY-200")).thenReturn(Optional.of(intent));
        when(intentRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        long ts = Instant.now().getEpochSecond();
        String body = "{\"paymentCode\":\"PAY-200\",\"amount\":1000000,\"status\":\"SUCCESS\",\"timestamp\":" + ts + "}";

        // Bad IP -> 403
        assertThatThrownBy(() -> service.processWebhook(body, hmac(body + "." + ts, "secret-a"), ts, "10.9.9.9"))
                .isInstanceOf(WebhookSecurityException.class)
                .extracting(e -> ((WebhookSecurityException) e).getHttpStatus()).isEqualTo(403);

        // Wrong HMAC -> 401
        assertThatThrownBy(() -> service.processWebhook(body, "0000deadbeef", ts, "127.0.0.1"))
                .isInstanceOf(WebhookSecurityException.class)
                .extracting(e -> ((WebhookSecurityException) e).getHttpStatus()).isEqualTo(401);

        // Valid webhook + duplicate replay
        assertThat(service.processWebhook(body, hmac(body + "." + ts, "secret-a"), ts, "127.0.0.1")).isTrue();
        assertThat(service.processWebhook(body, hmac(body + "." + ts, "secret-a"), ts, "127.0.0.1")).isTrue();
        assertThat(gateway.getBalance()).isEqualTo(1_000_000L);
        verify(outboxRepo, times(1)).save(any(BillingEventOutbox.class));

        // Reconciliation persists report
        when(intentRepo.findAll()).thenReturn(List.of(intent));
        when(reportRepo.findByReconDate(any())).thenReturn(Optional.empty());
        when(reportRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        PaymentReconciliationService reconService = new PaymentReconciliationService(
                intentRepo, ledger, alertPort, Optional.of(reportRepo), Optional.empty(), Optional.empty());
        var report = reconService.reconcileGatewaySettlements();
        assertThat(report.matched()).isTrue();
        verify(reportRepo).save(any(ReconciliationReport.class));
    }

    @Test
    void enforcesServerSideOtpMakerCheckerPayoutAndCumulativeRefundRetry() {
        BillingIdentityVerificationPort idPort = mock(BillingIdentityVerificationPort.class);
        when(idPort.verifyOtp("l1", "000000")).thenReturn(false);
        when(idPort.verifyOtp("l1", "123456")).thenReturn(true);

        PayoutRequestRepository poRepo = mock(PayoutRequestRepository.class);
        RiskProfileRepository riskRepo = mock(RiskProfileRepository.class);
        MakerCheckerRepository mcRepo = mock(MakerCheckerRepository.class);
        LedgerAccountRepository accRepo = mock(LedgerAccountRepository.class);
        JournalEntryRepository jeRepo = mock(JournalEntryRepository.class);

        LedgerAccount wallet = LedgerAccount.create("2020-WALLET-l1", "Wallet", AccountType.LIABILITY, "l1");
        wallet.applyPosting(PostingDirection.CREDIT, 20_000_000L);
        LedgerAccount gateway = LedgerAccount.create("1010-GATEWAY", "Gateway", AccountType.ASSET, null);
        when(accRepo.findByAccountNumber("2020-WALLET-l1")).thenReturn(Optional.of(wallet));
        when(accRepo.findByAccountNumberForUpdate("2020-WALLET-l1")).thenReturn(Optional.of(wallet));
        when(accRepo.findByAccountNumberForUpdate("1010-GATEWAY")).thenReturn(Optional.of(gateway));
        when(accRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jeRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(riskRepo.findByUserId("l1")).thenReturn(Optional.of(RiskProfile.createDefault("l1")));
        when(riskRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mcRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(poRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        LedgerService ledger = new LedgerService(accRepo, jeRepo, mock(AlertNotificationPort.class), mock(ReconciliationReportRepository.class));
        PayoutService payoutService = new PayoutService(
                poRepo, riskRepo, ledger, Optional.of(idPort), Optional.of(mcRepo), Optional.empty(),
                "SANDBOX_ONLY", false, 10_000_000L);

        // Wrong server-side OTP rejected
        assertThatThrownBy(() -> payoutService.requestPayout("l1",
                new PayoutDto.RequestPayoutCommand(2_000_000L, "VCB", "9988776655", "NGUYEN VAN L", "000000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OTP");

        // > 10M goes PENDING_APPROVAL and masks bank account number
        PayoutDto.PayoutResponse highPayout = payoutService.requestPayout("l1",
                new PayoutDto.RequestPayoutCommand(12_000_000L, "VCB", "9988776655", "NGUYEN VAN L", "123456"));
        assertThat(highPayout.status()).isEqualTo("PENDING_APPROVAL");
        assertThat(highPayout.accountNumber()).isEqualTo("****6655");

        // Refund cumulative guard + server KYC verification
        when(idPort.verifyOtp("t1", "654321")).thenReturn(true);
        when(idPort.getKycStatus("t1")).thenReturn(new BillingIdentityVerificationPort.KycVerificationSummary(
                "t1", "APPROVED", true, "TRAN MINH T"));
        RefundRequestRepository refRepo = mock(RefundRequestRepository.class);
        PaymentIntentRepository piRepo = mock(PaymentIntentRepository.class);
        PaymentIntent paidIntent = new PaymentIntent("pi-1", "PAY-1", "t1", "DEPOSIT", "cnt-1",
                3_000_000L, 3_000_000L, "qr", "PAID", Instant.now().plusSeconds(60), Instant.now(), Instant.now());
        when(piRepo.findById("pi-1")).thenReturn(Optional.of(paidIntent));
        when(refRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        RefundService refundService = new RefundService(
                refRepo, mcRepo, ledger, Optional.of(idPort), Optional.of(piRepo), Optional.empty(), "SANDBOX_ONLY", false);

        assertThatThrownBy(() -> refundService.requestRefund("t1", new RefundDto.CreateRefundCommand(
                "pi-1", "cnt-1", "l1", 4_000_000L, "VCB", "11223344", "TRAN MINH T", null, "654321", "Over")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vuot qua");
    }

    @Test
    void enforcesEscrowTwoPartyMoveInConfirmationAndIdor() {
        EscrowHoldRepository holdRepo = mock(EscrowHoldRepository.class);
        EscrowHold hold = EscrowHold.createHold("cnt-esc", "t-esc", "l-esc", 3_000_000L, true, 72);
        when(holdRepo.findByContractId("cnt-esc")).thenReturn(Optional.of(hold));
        when(holdRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        EscrowService escrowService = new EscrowService(mock(LedgerService.class), holdRepo, Optional.empty(), "SANDBOX_ONLY", false);

        assertThatThrownBy(() -> escrowService.getHoldForParticipant("cnt-esc", "stranger", false))
                .isInstanceOf(AccessDeniedException.class);
        escrowService.confirmMoveIn("cnt-esc", "t-esc", false);
        assertThat(hold.isBothConfirmedMoveIn()).isFalse();
        escrowService.confirmMoveIn("cnt-esc", "l-esc", false);
        assertThat(hold.isBothConfirmedMoveIn()).isTrue();
        assertThat(hold.getMoveInConfirmedAt()).isNotNull();
    }
}

