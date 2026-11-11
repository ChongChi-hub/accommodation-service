package vn.cozynest.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.port.AlertNotificationPort;
import vn.cozynest.billing.application.service.LedgerService;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.JournalEntryRepository;
import vn.cozynest.billing.domain.repository.LedgerAccountRepository;
import vn.cozynest.billing.domain.repository.ReconciliationReportRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LedgerServiceTest {

    @Test
    @DisplayName("LEDGER-01: Double-entry invariant holds when Sum(Debit) == Sum(Credit)")
    void testBalancedJournalEntry() {
        String entryId = "JE-TEST-001";
        LedgerPosting debit = LedgerPosting.create(entryId, "acc-1", PostingDirection.DEBIT, 5_000_000L);
        LedgerPosting credit = LedgerPosting.create(entryId, "acc-2", PostingDirection.CREDIT, 5_000_000L);

        JournalEntry entry = JournalEntry.createAndValidate(
                "JE-001", "Deposit payment", "DEPOSIT", "contract-1", "user-1",
                List.of(debit, credit)
        );

        assertNotNull(entry);
        assertEquals(5_000_000L, entry.getTotalAmount());
        assertEquals("POSTED", entry.getStatus());
    }

    @Test
    @DisplayName("LEDGER-01: Unbalanced journal entry must fail with IllegalStateException")
    void testUnbalancedJournalEntryFails() {
        String entryId = "JE-TEST-002";
        LedgerPosting debit = LedgerPosting.create(entryId, "acc-1", PostingDirection.DEBIT, 5_000_000L);
        LedgerPosting credit = LedgerPosting.create(entryId, "acc-2", PostingDirection.CREDIT, 4_000_000L); // Mismatch!

        assertThrows(IllegalStateException.class, () ->
                JournalEntry.createAndValidate("JE-002", "Unbalanced", "TEST", "ref-1", "user-1", List.of(debit, credit))
        );
    }

    @Test
    @DisplayName("LEDGER-01: Normal asset and liability balance calculations")
    void testAccountBalanceCalculations() {
        LedgerAccount gatewayAsset = LedgerAccount.create("1010-GATEWAY", "Gateway Cash", AccountType.ASSET, null);
        LedgerAccount escrowLiability = LedgerAccount.create("2010-ESCROW", "Deposit Escrow", AccountType.LIABILITY, null);

        gatewayAsset.applyPosting(PostingDirection.DEBIT, 3_000_000L);
        escrowLiability.applyPosting(PostingDirection.CREDIT, 3_000_000L);

        assertEquals(3_000_000L, gatewayAsset.getBalance());
        assertEquals(3_000_000L, escrowLiability.getBalance());
    }

    @Test
    @DisplayName("FIN-RISK-01: Frozen account blocks posting")
    void testFrozenAccountBlocksPosting() {
        LedgerAccount account = LedgerAccount.create("2020-WALLET-01", "Wallet", AccountType.LIABILITY, "user-1");
        account.freeze();

        assertThrows(IllegalStateException.class, () ->
                account.applyPosting(PostingDirection.CREDIT, 1_000_000L)
        );
    }

    @Test
    @DisplayName("LEDGER-02: Maker-checker cannot self-approve (cố tự duyệt điều chỉnh của chính mình bị từ chối)")
    void testMakerCheckerSelfApproveFails() {
        MakerCheckerRequest req = MakerCheckerRequest.create("admin-01", "MANUAL_ADJUSTMENT", 1_000_000L, "Fix balance", "{}");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> req.approve("admin-01"));
        assertTrue(ex.getMessage().contains("Maker-Checker"));
    }

    @Test
    @DisplayName("LEDGER-02: Maker-checker approves with distinct reviewer")
    void testMakerCheckerApproveSuccess() {
        MakerCheckerRequest req = MakerCheckerRequest.create("admin-01", "MANUAL_ADJUSTMENT", 1_000_000L, "Fix balance", "{}");
        req.approve("admin-02");

        assertEquals("APPROVED", req.getStatus());
        assertEquals("admin-02", req.getCheckerId());
        assertNotNull(req.getDecidedAt());
    }

    @Test
    @DisplayName("LEDGER-02: Reconciliation detects wallet balance mismatch when balance is altered without journal entry")
    void testReconciliationDetectsWalletBalanceMismatch() {
        LedgerAccountRepository accountRepo = mock(LedgerAccountRepository.class);
        JournalEntryRepository journalRepo = mock(JournalEntryRepository.class);
        AlertNotificationPort alertService = mock(AlertNotificationPort.class);
        ReconciliationReportRepository reconRepo = mock(ReconciliationReportRepository.class);

        // Tạo tài khoản ví có số dư lưu trong DB là 2,000,000 VND
        LedgerAccount wallet = LedgerAccount.create("2020-WALLET-USER1", "Ví chủ trọ", AccountType.LIABILITY, "user-1");
        // Giả sử có ai đó cố tình sửa trực tiếp số dư lên 3,000,000 mà không qua bút toán sổ cái!
        wallet.applyPosting(PostingDirection.CREDIT, 3_000_000L);

        when(accountRepo.findAll()).thenReturn(List.of(wallet));

        // Nhật ký bút toán thực tế chỉ có 2,000,000 VND
        String entryId = "JE-001";
        LedgerPosting p1 = LedgerPosting.create(entryId, wallet.getId(), PostingDirection.CREDIT, 2_000_000L);
        LedgerPosting p2 = LedgerPosting.create(entryId, "gateway-id", PostingDirection.DEBIT, 2_000_000L);
        JournalEntry entry = JournalEntry.createAndValidate("JE-001", "Payment", "PAY", "p-1", "sys", List.of(p1, p2));

        when(journalRepo.findAll()).thenReturn(List.of(entry));

        LedgerService service = new LedgerService(accountRepo, journalRepo, alertService, reconRepo);
        LedgerDto.ComprehensiveIntegrityReport report = service.runDailyReconciliation();

        // Kiểm tra đối soát: phải lập tức phát hiện mismatch!
        assertFalse(report.isSystemBalanced(), "Hệ thống phải báo KHÔNG cân đối khi số dư ví không khớp bút toán");
        assertEquals(1, report.totalMismatchedAccounts(), "Phải có 1 tài khoản ví bị lệch");

        LedgerDto.WalletReconciliationItem walletItem = report.walletBreakdowns().get(0);
        assertFalse(walletItem.isMatched());
        assertEquals(3_000_000L, walletItem.storedBalance());
        assertEquals(2_000_000L, walletItem.calculatedBalance());
        assertEquals(1_000_000L, walletItem.difference());

        // Kiểm tra cảnh báo Telegram/Slack đã được kích hoạt
        verify(alertService, atLeastOnce()).sendAlert(anyString(), anyString());
    }

    @Test
    @DisplayName("LEDGER-02: Export General Ledger and Trial Balance produces valid CSV with UTF-8 BOM")
    void testExportAccountingReportsCsvWithUtf8Bom() {
        LedgerAccountRepository accountRepo = mock(LedgerAccountRepository.class);
        JournalEntryRepository journalRepo = mock(JournalEntryRepository.class);
        AlertNotificationPort alertService = mock(AlertNotificationPort.class);
        ReconciliationReportRepository reconRepo = mock(ReconciliationReportRepository.class);

        LedgerAccount acc1 = LedgerAccount.create("1010-GATEWAY", "Tiền gửi cổng thanh toán", AccountType.ASSET, null);
        LedgerAccount acc2 = LedgerAccount.create("4010-REVENUE", "Doanh thu dịch vụ", AccountType.REVENUE, null);

        when(accountRepo.findAll()).thenReturn(List.of(acc1, acc2));

        String entryId = "JE-TEST-BOM";
        LedgerPosting debit = LedgerPosting.create(entryId, acc1.getId(), PostingDirection.DEBIT, 500_000L);
        LedgerPosting credit = LedgerPosting.create(entryId, acc2.getId(), PostingDirection.CREDIT, 500_000L);
        JournalEntry entry = JournalEntry.createAndValidate("JE-001", "Phí dịch vụ VIP", "FEE", "fee-1", "admin-1", List.of(debit, credit));

        when(journalRepo.findAll()).thenReturn(List.of(entry));

        LedgerService service = new LedgerService(accountRepo, journalRepo, alertService, reconRepo);

        String glCsv = service.exportGeneralLedgerCsv(null, null);
        assertTrue(glCsv.startsWith("\uFEFF"), "Sổ cái CSV phải bắt đầu bằng UTF-8 BOM để Excel không lỗi font tiếng Việt");
        assertTrue(glCsv.contains("1010-GATEWAY"));
        assertTrue(glCsv.contains("500000"));

        String tbCsv = service.exportTrialBalanceCsv(null, null);
        assertTrue(tbCsv.startsWith("\uFEFF"), "Bảng cân đối thử CSV phải bắt đầu bằng UTF-8 BOM");
        assertTrue(tbCsv.contains("TỔNG CỘNG"));
    }
}
