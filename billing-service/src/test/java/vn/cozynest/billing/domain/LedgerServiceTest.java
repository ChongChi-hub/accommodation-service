package vn.cozynest.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.billing.domain.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    @DisplayName("LEDGER-02: Maker-checker cannot self-approve")
    void testMakerCheckerSelfApproveFails() {
        MakerCheckerRequest req = MakerCheckerRequest.create("admin-01", "MANUAL_ADJUSTMENT", 1_000_000L, "Fix balance", "{}");

        assertThrows(IllegalStateException.class, () -> req.approve("admin-01"));
    }

    @Test
    @DisplayName("LEDGER-02: Maker-checker approves with distinct reviewer")
    void testMakerCheckerApproveSuccess() {
        MakerCheckerRequest req = MakerCheckerRequest.create("admin-01", "MANUAL_ADJUSTMENT", 1_000_000L, "Fix balance", "{}");
        req.approve("admin-02");

        assertEquals("APPROVED", req.getStatus());
        assertEquals("admin-02", req.getCheckerId());
    }

    @Test
    @DisplayName("METER-01: Anomaly surge alert triggers on 2x average consumption")
    void testUtilityReadingSurgeAnomaly() {
        UtilityReading normalReading = new UtilityReading(
                "ur-1", "ct-1", "room-1", LocalDate.of(2026, 11, 1),
                new BigDecimal("100"), new BigDecimal("180"), // 80 kWh
                new BigDecimal("10"), new BigDecimal("18"),   // 8 m3
                "[]", "landlord-1", Instant.now()
        );
        assertFalse(normalReading.isSurgeAnomaly(new BigDecimal("100"), new BigDecimal("10")));

        UtilityReading surgeReading = new UtilityReading(
                "ur-2", "ct-1", "room-1", LocalDate.of(2026, 11, 1),
                new BigDecimal("100"), new BigDecimal("350"), // 250 kWh (> 200 avg)
                new BigDecimal("10"), new BigDecimal("18"),
                "[]", "landlord-1", Instant.now()
        );
        assertTrue(surgeReading.isSurgeAnomaly(new BigDecimal("100"), new BigDecimal("10")));
    }
}

