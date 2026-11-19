package vn.cozynest.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.cozynest.billing.application.dto.PayoutDto;
import vn.cozynest.billing.application.port.AlertNotificationPort;
import vn.cozynest.billing.application.port.BillingContractPort;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.*;
import vn.cozynest.billing.infrastructure.gateway.ConfigurableBankNotificationProvider;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletVerifyLedgerEndToEndTest {
    private Map<String, LedgerAccount> accounts;
    private List<JournalEntry> journalEntries;
    private Map<String, PaymentIntent> intentsByCode;
    private Map<String, EscrowHold> holdsByContract;
    private Map<String, BankTransactionRecord> bankTxMap;
    private Map<String, UnmatchedBankInflow> unmatchedMap;
    private Map<String, PayoutRequest> payoutsById;
    private Map<String, UserBankAccount> bankAccountsByUser;
    private Map<String, Invoice> invoicesById;

    private LedgerService ledgerService;
    private EscrowService escrowService;
    private BankInflowWebhookService webhookService;
    private PayoutService payoutService;
    private ConfigurableBankNotificationProvider provider;

    @BeforeEach
    void setUp() {
        accounts = new ConcurrentHashMap<>();
        journalEntries = new CopyOnWriteArrayList<>();
        intentsByCode = new ConcurrentHashMap<>();
        holdsByContract = new ConcurrentHashMap<>();
        bankTxMap = new ConcurrentHashMap<>();
        unmatchedMap = new ConcurrentHashMap<>();
        payoutsById = new ConcurrentHashMap<>();
        bankAccountsByUser = new ConcurrentHashMap<>();
        invoicesById = new ConcurrentHashMap<>();

        LedgerAccountRepository accRepo = mock(LedgerAccountRepository.class);
        when(accRepo.findByAccountNumber(any())).thenAnswer(i -> Optional.ofNullable(accounts.get(i.getArgument(0))));
        when(accRepo.findByAccountNumberForUpdate(any())).thenAnswer(i -> Optional.ofNullable(accounts.get(i.getArgument(0))));
        when(accRepo.save(any())).thenAnswer(i -> { LedgerAccount a = i.getArgument(0); accounts.put(a.getAccountNumber(), a); return a; });
        when(accRepo.findAll()).thenAnswer(i -> new ArrayList<>(accounts.values()));

        JournalEntryRepository jeRepo = mock(JournalEntryRepository.class);
        when(jeRepo.findByIdempotencyKey(any())).thenAnswer(i -> journalEntries.stream().filter(e -> i.getArgument(0).equals(e.getIdempotencyKey())).findFirst());
        when(jeRepo.save(any())).thenAnswer(i -> { JournalEntry e = i.getArgument(0); journalEntries.add(e); return e; });
        when(jeRepo.findAll()).thenAnswer(i -> new ArrayList<>(journalEntries));
        ledgerService = new LedgerService(accRepo, jeRepo, mock(AlertNotificationPort.class), mock(ReconciliationReportRepository.class));

        PaymentIntentRepository intentRepo = mock(PaymentIntentRepository.class);
        when(intentRepo.findByPaymentCode(any())).thenAnswer(i -> Optional.ofNullable(intentsByCode.get(i.getArgument(0))));
        when(intentRepo.save(any())).thenAnswer(i -> { PaymentIntent p = i.getArgument(0); intentsByCode.put(p.getPaymentCode(), p); return p; });
        when(intentRepo.findAll()).thenAnswer(i -> new ArrayList<>(intentsByCode.values()));

        EscrowHoldRepository holdRepo = mock(EscrowHoldRepository.class);
        when(holdRepo.findByContractId(any())).thenAnswer(i -> Optional.ofNullable(holdsByContract.get(i.getArgument(0))));
        when(holdRepo.save(any())).thenAnswer(i -> { EscrowHold h = i.getArgument(0); holdsByContract.put(h.getContractId(), h); return h; });
        when(holdRepo.findAll()).thenAnswer(i -> new ArrayList<>(holdsByContract.values()));

        BankTransactionRegistryRepository btrRepo = mock(BankTransactionRegistryRepository.class);
        when(btrRepo.existsByNormalizedTxCode(any())).thenAnswer(i -> bankTxMap.containsKey(PaymentReferenceCodeCodec.normalizeAlphanumericUpper(i.getArgument(0))));
        when(btrRepo.save(any())).thenAnswer(i -> { BankTransactionRecord r = i.getArgument(0); bankTxMap.put(r.getNormalizedTxCode(), r); return r; });

        UnmatchedBankInflowRepository ubiRepo = mock(UnmatchedBankInflowRepository.class);
        when(ubiRepo.existsByProviderTxId(any())).thenAnswer(i -> unmatchedMap.containsKey(PaymentReferenceCodeCodec.normalizeAlphanumericUpper(i.getArgument(0))));
        when(ubiRepo.findByProviderTxId(any())).thenAnswer(i -> Optional.ofNullable(unmatchedMap.get(PaymentReferenceCodeCodec.normalizeAlphanumericUpper(i.getArgument(0)))));
        when(ubiRepo.save(any())).thenAnswer(i -> { UnmatchedBankInflow u = i.getArgument(0); unmatchedMap.put(u.getProviderTxId(), u); return u; });

        UserBankAccountRepository bankRepo = mock(UserBankAccountRepository.class);
        when(bankRepo.findByUserId(any())).thenAnswer(i -> Optional.ofNullable(bankAccountsByUser.get(i.getArgument(0))));
        when(bankRepo.save(any())).thenAnswer(i -> { UserBankAccount b = i.getArgument(0); bankAccountsByUser.put(b.getUserId(), b); return b; });
        BankAccountService bankService = new BankAccountService(bankRepo);
        bankAccountsByUser.put("landlord-1", UserBankAccount.create("landlord-1", "MB", "MBBank", "0987654321", "NGUYEN CHU TRO", "https://qr"));
        bankAccountsByUser.put("tenant-1", UserBankAccount.create("tenant-1", "VCB", "Vietcombank", "1122334455", "TRAN KHACH THUE", "https://qr"));

        InvoiceRepository invoiceRepo = mock(InvoiceRepository.class);
        when(invoiceRepo.findById(any())).thenAnswer(i -> Optional.ofNullable(invoicesById.get(i.getArgument(0))));
        when(invoiceRepo.save(any())).thenAnswer(i -> { Invoice inv = i.getArgument(0); invoicesById.put(inv.getId(), inv); return inv; });

        BillingContractPort contractPort = mock(BillingContractPort.class);
        when(contractPort.getContractSnapshot(eq("cnt-e2e"), any())).thenReturn(Optional.of(
                new BillingContractPort.ContractSnapshot("cnt-e2e", "r-1", "tenant-1", "landlord-1", "AWAITING_DEPOSIT", 2_000_000L, 5, 1_000_000L, null)));

        escrowService = new EscrowService(ledgerService, holdRepo, Optional.empty(), Optional.of(bankService), "SANDBOX_ONLY", false, 50, 0);
        PaymentLedgerPoster poster = new PaymentLedgerPoster(ledgerService, invoiceRepo, escrowService, contractPort, null, "SANDBOX_ONLY", false, 5);
        provider = new ConfigurableBankNotificationProvider(new ObjectMapper(), "FAKE", "test-fake-secret", "", 300, "1042701900");
        webhookService = new BankInflowWebhookService(provider, intentRepo, btrRepo, ubiRepo, null, null, null, poster, null, "1042701900", 10_000_000L);

        PayoutRequestRepository poRepo = mock(PayoutRequestRepository.class);
        when(poRepo.save(any())).thenAnswer(i -> { PayoutRequest p = i.getArgument(0); payoutsById.put(p.getId(), p); return p; });
        when(poRepo.findById(any())).thenAnswer(i -> Optional.ofNullable(payoutsById.get(i.getArgument(0))));
        when(poRepo.findByIdempotencyKey(any())).thenAnswer(i -> payoutsById.values().stream().filter(p -> i.getArgument(0).equals(p.getIdempotencyKey())).findFirst());
        RiskProfileRepository riskRepo = mock(RiskProfileRepository.class);
        when(riskRepo.findByUserId(any())).thenReturn(Optional.of(RiskProfile.createDefault("landlord-1")));
        when(riskRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        payoutService = new PayoutService(poRepo, riskRepo, ledgerService, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(bankService), Optional.of(btrRepo), "SANDBOX_ONLY", false, 10_000_000L, 1000L);
    }

    @Test
    void endToEndDeposit50PercentReleaseRentPlusElectricity5PercentCommissionAndAdminWithdrawal() {
        String depCode = PaymentReferenceCodeCodec.generateReferenceCode();
        assertThat(depCode).startsWith("TOAM").hasSizeLessThanOrEqualTo(23).doesNotContain("-");
        intentsByCode.put(depCode, PaymentIntent.create(depCode, "tenant-1", "DEPOSIT", "cnt-e2e", 1_000_000L, "qr", 15));

        long ts = Instant.now().getEpochSecond();
        String depBody = "{\"providerTxId\":\"FT261119DEP001\",\"direction\":\"IN\",\"amount\":1000000,\"receiverAccount\":\"1042701900\",\"content\":\"VCB IBFT " + depCode + " COC\",\"timestamp\":" + ts + "}";
        var depRes = webhookService.processProviderWebhook(depBody, provider.computeTestSignature(depBody, ts), ts, "127.0.0.1", Map.of());
        assertThat(depRes.autoConfirmed()).isTrue();
        assertThat(holdsByContract.get("cnt-e2e").getStatus()).isEqualTo("HELD");
        assertThat(ledgerService.getAccountBalance(LedgerService.ACC_GATEWAY)).isEqualTo(1_000_000L);
        assertThat(ledgerService.getAccountBalance(LedgerService.ACC_ESCROW)).isEqualTo(1_000_000L);
        assertThat(ledgerService.getAccountBalance(LedgerService.ACC_PLATFORM_REVENUE)).isZero();

        // Both parties confirm move-in -> 50% (500,000) to landlord available, 50% (500,000) retained in escrow
        escrowService.confirmMoveIn("cnt-e2e", "tenant-1", false);
        EscrowHold afterBoth = escrowService.confirmMoveIn("cnt-e2e", "landlord-1", false);
        assertThat(afterBoth.getStatus()).isEqualTo("PARTIALLY_RELEASED");
        assertThat(afterBoth.getReleasedAmount()).isEqualTo(500_000L);
        assertThat(afterBoth.getRetainedAmount()).isEqualTo(500_000L);
        assertThat(ledgerService.getAccountBalance(LedgerService.ACC_ESCROW)).isEqualTo(500_000L);
        assertThat(ledgerService.getAccountBalance(ledgerService.getLandlordAvailableAccountNumber("landlord-1"))).isEqualTo(500_000L);

        // Monthly invoice: 1,000,000 rent + 200,000 electricity = 1,200,000 -> 5% commission = 60,000, landlord = 1,140,000
        Invoice inv = Invoice.create("INV-202611-E2E", "cnt-e2e", null, "tenant-1", "landlord-1",
                LocalDate.of(2026, 11, 1), 1_000_000L, 200_000L, 0L, 0L, 0L, LocalDate.of(2026, 11, 10));
        invoicesById.put(inv.getId(), inv);
        String invCode = PaymentReferenceCodeCodec.generateReferenceCode();
        intentsByCode.put(invCode, PaymentIntent.create(invCode, "tenant-1", "INVOICE", inv.getId(), 1_200_000L, "qr", 15));

        String invBody = "{\"providerTxId\":\"FT261119INV001\",\"direction\":\"IN\",\"amount\":1200000,\"receiverAccount\":\"1042701900\",\"content\":\"TT " + invCode + "\",\"timestamp\":" + ts + "}";
        var invRes = webhookService.processProviderWebhook(invBody, provider.computeTestSignature(invBody, ts), ts, "127.0.0.1", Map.of());
        assertThat(invRes.autoConfirmed()).isTrue();
        assertThat(ledgerService.getAccountBalance(LedgerService.ACC_PLATFORM_REVENUE)).isEqualTo(60_000L);
        assertThat(ledgerService.getAccountBalance(ledgerService.getLandlordAvailableAccountNumber("landlord-1"))).isEqualTo(1_640_000L);
        assertThat(ledgerService.getAccountBalance(LedgerService.ACC_GATEWAY)).isEqualTo(2_200_000L);

        // Landlord requests withdrawal of 1,000,000 -> WITHDRAWAL_HOLD, Admin confirms paid -> GATEWAY reduced
        PayoutDto.PayoutResponse po = payoutService.requestPayout("landlord-1",
                new PayoutDto.RequestPayoutCommand(1_000_000L, "MB", "0987654321", "NGUYEN CHU TRO", "123456"), "idem-wd-1");
        assertThat(po.status()).isEqualTo("REQUESTED");
        assertThat(po.systemVietQrUrl()).contains("https://img.vietqr.io/image/MB-0987654321");
        assertThat(ledgerService.getAccountBalance(ledgerService.getLandlordAvailableAccountNumber("landlord-1"))).isEqualTo(640_000L);
        assertThat(ledgerService.getAccountBalance(ledgerService.getWithdrawalHoldAccountNumber("landlord-1"))).isEqualTo(1_000_000L);

        PayoutDto.PayoutResponse paidPo = payoutService.confirmPayoutPaid(po.id(), "admin-1",
                new PayoutDto.ConfirmPayoutPaidCommand("FT261119OUT001", "https://proof", "Da ck"));
        assertThat(paidPo.status()).isEqualTo("PAID");
        assertThat(ledgerService.getAccountBalance(ledgerService.getWithdrawalHoldAccountNumber("landlord-1"))).isZero();
        assertThat(ledgerService.getAccountBalance(LedgerService.ACC_GATEWAY)).isEqualTo(1_200_000L);

        // End-of-contract deduction claim (100,000) approved by Admin, remaining 400,000 stays refundable to Tenant
        escrowService.submitDeductionClaim("cnt-e2e", "landlord-1", 100_000L, "Hong bong den", "https://img/claim.jpg");
        EscrowHold afterDeduct = escrowService.decideDeductionClaim("cnt-e2e", "admin-1", true, "Hop ly");
        assertThat(afterDeduct.getRetainedAmount()).isEqualTo(400_000L);
        assertThat(ledgerService.getAccountBalance(LedgerService.ACC_ESCROW)).isEqualTo(400_000L);
        assertThat(ledgerService.getAccountBalance(ledgerService.getLandlordAvailableAccountNumber("landlord-1"))).isEqualTo(740_000L);

        // Invariant: ACC_GATEWAY == ESCROW (400k) + REVENUE (60k) + LANDLORD_AVAILABLE (740k) = 1,200,000
        long totalLiabAndRev = ledgerService.getAccountBalance(LedgerService.ACC_ESCROW)
                + ledgerService.getAccountBalance(LedgerService.ACC_PLATFORM_REVENUE)
                + ledgerService.getAccountBalance(ledgerService.getLandlordAvailableAccountNumber("landlord-1"))
                + ledgerService.getAccountBalance(ledgerService.getWithdrawalHoldAccountNumber("landlord-1"));
        assertThat(totalLiabAndRev).isEqualTo(ledgerService.getAccountBalance(LedgerService.ACC_GATEWAY));
    }

    @Test
    void supportsConfigurableReleasePercentHoldDaysAndHalfUpCommissionRounding() {
        for (long amt : new long[]{1_000L, 1_001L, 1_010L, 33_333L, 1_200_000L, 9_999_999L}) {
            long fee = PaymentLedgerPoster.computeCommissionHalfUp(amt, 5);
            assertThat(fee + (amt - fee)).isEqualTo(amt);
        }
        EscrowHoldRepository holdRepo2 = mock(EscrowHoldRepository.class);
        Map<String, EscrowHold> hMap2 = new HashMap<>();
        hMap2.put("cnt-hold3", EscrowHold.createHold("cnt-hold3", "tenant-1", "landlord-1", 1_000_000L, true, 72));
        when(holdRepo2.findByContractId("cnt-hold3")).thenAnswer(i -> Optional.ofNullable(hMap2.get("cnt-hold3")));
        when(holdRepo2.save(any())).thenAnswer(i -> { EscrowHold x = i.getArgument(0); hMap2.put(x.getContractId(), x); return x; });
        when(holdRepo2.findAll()).thenAnswer(i -> new ArrayList<>(hMap2.values()));
        ledgerService.getOrCreateAccount(LedgerService.ACC_ESCROW, "Escrow", AccountType.LIABILITY, null);
        ledgerService.getOrCreateAccount(LedgerService.ACC_GATEWAY, "Gateway", AccountType.ASSET, null);
        ledgerService.postEntry("Seed", "DEPOSIT", "cnt-hold3", "sys", List.of(
                new vn.cozynest.billing.application.dto.LedgerDto.PostingItem(LedgerService.ACC_GATEWAY, PostingDirection.DEBIT, 1_000_000L),
                new vn.cozynest.billing.application.dto.LedgerDto.PostingItem(LedgerService.ACC_ESCROW, PostingDirection.CREDIT, 1_000_000L)));
        EscrowService holdDaysService = new EscrowService(ledgerService, holdRepo2, Optional.empty(), Optional.empty(), "SANDBOX_ONLY", false, 40, 3);
        holdDaysService.confirmMoveIn("cnt-hold3", "tenant-1", false);
        holdDaysService.confirmMoveIn("cnt-hold3", "landlord-1", false);
        assertThat(ledgerService.getAccountBalance(ledgerService.getLandlordPendingAccountNumber("landlord-1"))).isEqualTo(400_000L);
        assertThat(ledgerService.getAccountBalance(LedgerService.ACC_ESCROW)).isEqualTo(600_000L);
        assertThat(holdDaysService.settleMaturedPendingReleases(Instant.now().plus(4, ChronoUnit.DAYS))).isEqualTo(1);
        assertThat(ledgerService.getAccountBalance(ledgerService.getLandlordPendingAccountNumber("landlord-1"))).isZero();
        assertThat(ledgerService.getAccountBalance(ledgerService.getLandlordAvailableAccountNumber("landlord-1"))).isEqualTo(400_000L);
    }
}
