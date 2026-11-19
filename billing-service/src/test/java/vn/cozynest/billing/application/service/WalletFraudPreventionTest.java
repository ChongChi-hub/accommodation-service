package vn.cozynest.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.cozynest.billing.application.dto.PayoutDto;
import vn.cozynest.billing.application.exception.WebhookSecurityException;
import vn.cozynest.billing.application.port.AlertNotificationPort;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.*;
import vn.cozynest.billing.infrastructure.gateway.ConfigurableBankNotificationProvider;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletFraudPreventionTest {
    @TempDir Path tempDir;

    @Test
    void blocksOldReceiptReScreenshotCaseInsensitiveTxCodeBadWebhookAndConcurrentDoubleWithdrawal() throws Exception {
        Map<String, PaymentProofRecord> proofs = new ConcurrentHashMap<>();
        Map<String, BankTransactionRecord> bankTxs = new ConcurrentHashMap<>();
        Map<String, UnmatchedBankInflow> unmatched = new ConcurrentHashMap<>();
        Map<String, PaymentIntent> intents = new ConcurrentHashMap<>();

        PaymentProofRegistryRepository proofRepo = mock(PaymentProofRegistryRepository.class);
        when(proofRepo.existsByNormalizedBankTxCode(any())).thenAnswer(i -> proofs.values().stream().anyMatch(p -> p.getNormalizedBankTxCode().equals(PaymentReferenceCodeCodec.normalizeAlphanumericUpper(i.getArgument(0)))));
        when(proofRepo.existsBySha256Hash(any())).thenAnswer(i -> proofs.values().stream().anyMatch(p -> p.getSha256Hash().equals(i.getArgument(0))));
        when(proofRepo.findAll()).thenAnswer(i -> new ArrayList<>(proofs.values()));
        when(proofRepo.findLatestByPaymentCode(any())).thenAnswer(i -> proofs.values().stream().filter(p -> p.getPaymentCode().equals(i.getArgument(0))).findFirst());
        when(proofRepo.save(any())).thenAnswer(i -> { PaymentProofRecord r = i.getArgument(0); proofs.put(r.getId(), r); return r; });
        when(proofRepo.saveAccessLog(any())).thenAnswer(i -> i.getArgument(0));

        BankTransactionRegistryRepository btrRepo = mock(BankTransactionRegistryRepository.class);
        when(btrRepo.existsByNormalizedTxCode(any())).thenAnswer(i -> bankTxs.containsKey(PaymentReferenceCodeCodec.normalizeAlphanumericUpper(i.getArgument(0))));
        when(btrRepo.save(any())).thenAnswer(i -> { BankTransactionRecord r = i.getArgument(0); bankTxs.put(r.getNormalizedTxCode(), r); return r; });

        UnmatchedBankInflowRepository ubiRepo = mock(UnmatchedBankInflowRepository.class);
        when(ubiRepo.existsByProviderTxId(any())).thenAnswer(i -> unmatched.containsKey(PaymentReferenceCodeCodec.normalizeAlphanumericUpper(i.getArgument(0))));
        when(ubiRepo.save(any())).thenAnswer(i -> { UnmatchedBankInflow u = i.getArgument(0); unmatched.put(u.getProviderTxId(), u); return u; });

        PaymentIntentRepository intentRepo = mock(PaymentIntentRepository.class);
        when(intentRepo.findByPaymentCode(any())).thenAnswer(i -> Optional.ofNullable(intents.get(i.getArgument(0))));
        when(intentRepo.findAll()).thenAnswer(i -> new ArrayList<>(intents.values()));

        PaymentProofStorageService proofService = new PaymentProofStorageService(proofRepo, btrRepo, null, intentRepo, null, tempDir.toString(), "aes-key-2026");

        // 1. Magic bytes validation blocks forged file
        assertThatThrownBy(() -> proofService.storeSupportingProof("TOAM1", "DEPOSIT", "cnt-1", "tenant-1", "FT111111", null,
                "data:image/png;base64," + Base64.getEncoder().encodeToString("NOT_A_REAL_IMAGE".getBytes())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Magic Bytes");

        // 2. Original proof accepted; case-insensitive/hyphen tx code replay & dHash re-screenshot blocked
        String origB64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(createSampleReceiptImage("png", 0));
        String reShotB64 = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(createSampleReceiptImage("jpg", 2));
        PaymentProofRecord savedProof = proofService.storeSupportingProof("TOAM1", "DEPOSIT", "cnt-1", "tenant-1", "FT-261119-AAA", null, origB64);
        assertThat(savedProof.getNormalizedBankTxCode()).isEqualTo("FT261119AAA");

        assertThatThrownBy(() -> proofService.storeSupportingProof("TOAM2", "DEPOSIT", "cnt-2", "tenant-1", "ft261119aaa", null, reShotB64))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("FT261119AAA");
        assertThatThrownBy(() -> proofService.storeSupportingProof("TOAM2", "DEPOSIT", "cnt-2", "tenant-1", "FT261119BBB", null, reShotB64))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("dHash");

        // 3. Proof IDOR: stranger -> 404, tenant/admin -> 200
        assertThatThrownBy(() -> proofService.loadAuthorizedProofImage("TOAM1", "stranger", false, "ROLE_TENANT", "127.0.0.1"))
                .isInstanceOf(WebhookSecurityException.class).extracting(e -> ((WebhookSecurityException) e).getHttpStatus()).isEqualTo(404);
        assertThat(proofService.loadAuthorizedProofImage("TOAM1", "tenant-1", false, "ROLE_TENANT", "127.0.0.1").imageBytes().length).isGreaterThan(100);

        // 4. Webhook: bad signature (401), wrong amount -> queued unmatched (200), replay -> ignored
        ConfigurableBankNotificationProvider provider = new ConfigurableBankNotificationProvider(new ObjectMapper(), "FAKE", "secret-key", "", 300, "1042701900");
        BankInflowWebhookService webhookService = new BankInflowWebhookService(provider, intentRepo, btrRepo, ubiRepo, null, null, null, mock(PaymentLedgerPoster.class), null, "1042701900", 10_000_000L);
        String code = PaymentReferenceCodeCodec.generateReferenceCode();
        intents.put(code, PaymentIntent.create(code, "tenant-1", "DEPOSIT", "cnt-f", 10_000L, "qr", 15));
        long ts = Instant.now().getEpochSecond();
        String misBody = "{\"providerTxId\":\"FT261119MIS01\",\"direction\":\"IN\",\"amount\":9000,\"receiverAccount\":\"1042701900\",\"content\":\"" + code + "\",\"timestamp\":" + ts + "}";

        assertThatThrownBy(() -> webhookService.processProviderWebhook(misBody, "bad-sig", ts, "127.0.0.1", Map.of()))
                .isInstanceOf(WebhookSecurityException.class).extracting(e -> ((WebhookSecurityException) e).getHttpStatus()).isEqualTo(401);
        var misOut = webhookService.processProviderWebhook(misBody, provider.computeTestSignature(misBody, ts), ts, "127.0.0.1", Map.of());
        assertThat(misOut.autoConfirmed()).isFalse();
        assertThat(misOut.status()).isEqualTo("QUEUED_UNMATCHED");
        var replayOut = webhookService.processProviderWebhook(misBody, provider.computeTestSignature(misBody, ts), ts, "127.0.0.1", Map.of());
        assertThat(replayOut.status()).isEqualTo("IDEMPOTENT_REPLAY_IGNORED");

        // 5. Concurrent double withdrawal race & non-MATCHED bank account rejection
        Map<String, LedgerAccount> accs = new ConcurrentHashMap<>();
        LedgerAccount wallet = LedgerAccount.create("2020-WALLET-ll-race", "Wallet", AccountType.LIABILITY, "ll-race");
        wallet.applyPosting(PostingDirection.CREDIT, 500_000L);
        accs.put(wallet.getAccountNumber(), wallet);
        LedgerAccountRepository accRepo = mock(LedgerAccountRepository.class);
        when(accRepo.findByAccountNumber(any())).thenAnswer(i -> Optional.ofNullable(accs.get(i.getArgument(0))));
        when(accRepo.findByAccountNumberForUpdate(any())).thenAnswer(i -> Optional.ofNullable(accs.get(i.getArgument(0))));
        when(accRepo.save(any())).thenAnswer(i -> { LedgerAccount a = i.getArgument(0); accs.put(a.getAccountNumber(), a); return a; });
        JournalEntryRepository jeRepo = mock(JournalEntryRepository.class);
        when(jeRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        LedgerService ledger = new LedgerService(accRepo, jeRepo, mock(AlertNotificationPort.class), mock(ReconciliationReportRepository.class));

        UserBankAccountRepository bankRepo = mock(UserBankAccountRepository.class);
        when(bankRepo.findByUserId("ll-race")).thenReturn(Optional.of(UserBankAccount.create("ll-race", "MB", "MB", "0987654321", "CHU TRO", "qr")));
        when(bankRepo.findByUserId("ll-unmatched")).thenReturn(Optional.of(new UserBankAccount("b2", "ll-unmatched", "MB", "MB", "12345678", "TEST", "qr", "PENDING", Instant.now(), Instant.now())));
        PayoutRequestRepository poRepo = mock(PayoutRequestRepository.class);
        when(poRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        RiskProfileRepository riskRepo = mock(RiskProfileRepository.class);
        when(riskRepo.findByUserId(any())).thenReturn(Optional.of(RiskProfile.createDefault("ll-race")));
        when(riskRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        PayoutService payoutService = new PayoutService(poRepo, riskRepo, ledger, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(new BankAccountService(bankRepo)), Optional.of(btrRepo), "SANDBOX_ONLY", false, 10_000_000L, 1000L);

        assertThatThrownBy(() -> payoutService.requestPayout("ll-unmatched",
                new PayoutDto.RequestPayoutCommand(100_000L, "MB", "12345678", "TEST", "123456")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("MATCHED");

        // Serialized synchronized race simulating DB row lock FOR UPDATE -> only 1 of 2 parallel 500k withdrawals succeeds
        Object rowLock = new Object();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            gate.await(3, TimeUnit.SECONDS);
            synchronized (rowLock) {
                try {
                    payoutService.requestPayout("ll-race", new PayoutDto.RequestPayoutCommand(500_000L, "MB", "0987654321", "CHU TRO", "123456"));
                    return true;
                } catch (IllegalStateException ex) {
                    return false;
                }
            }
        };
        Future<Boolean> f1 = pool.submit(attempt);
        Future<Boolean> f2 = pool.submit(attempt);
        gate.countDown();
        int successes = (f1.get(5, TimeUnit.SECONDS) ? 1 : 0) + (f2.get(5, TimeUnit.SECONDS) ? 1 : 0);
        pool.shutdownNow();
        assertThat(successes).isEqualTo(1);
        assertThat(ledger.getAccountBalance("2020-WALLET-ll-race")).isZero();
    }

    private byte[] createSampleReceiptImage(String fmt, int noiseShift) throws Exception {
        BufferedImage img = new BufferedImage(120, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(20 + noiseShift, 40, 60));
        g.fillRect(0, 0, 60, 120);
        g.setColor(new Color(230 - noiseShift, 220, 210));
        g.fillRect(60, 0, 60, 120);
        g.setColor(Color.RED);
        g.fillRect(20, 20, 40, 40);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, fmt, baos);
        return baos.toByteArray();
    }
}
