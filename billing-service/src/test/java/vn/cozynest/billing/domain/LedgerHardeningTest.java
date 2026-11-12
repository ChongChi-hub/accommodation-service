package vn.cozynest.billing.domain;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import vn.cozynest.billing.application.dto.LedgerDto;
import vn.cozynest.billing.application.port.AlertNotificationPort;
import vn.cozynest.billing.application.service.LedgerService;
import vn.cozynest.billing.domain.model.*;
import vn.cozynest.billing.domain.repository.JournalEntryRepository;
import vn.cozynest.billing.domain.repository.LedgerAccountRepository;
import vn.cozynest.billing.domain.repository.ReconciliationReportRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LedgerHardeningTest {

    @Test
    void acquiresPessimisticLocksInDeterministicSortedOrderAndDeduplicatesByIdempotencyKey() {
        LedgerAccountRepository accountRepo = mock(LedgerAccountRepository.class);
        JournalEntryRepository journalRepo = mock(JournalEntryRepository.class);
        AlertNotificationPort alertPort = mock(AlertNotificationPort.class);
        ReconciliationReportRepository reconRepo = mock(ReconciliationReportRepository.class);

        LedgerAccount gateway = LedgerAccount.create("1010-GATEWAY", "Gateway", AccountType.ASSET, null);
        LedgerAccount escrow = LedgerAccount.create("2010-ESCROW", "Escrow", AccountType.LIABILITY, null);

        when(journalRepo.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(accountRepo.findByAccountNumberForUpdate("1010-GATEWAY")).thenReturn(Optional.of(gateway));
        when(accountRepo.findByAccountNumberForUpdate("2010-ESCROW")).thenReturn(Optional.of(escrow));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(journalRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LedgerService service = new LedgerService(accountRepo, journalRepo, alertPort, reconRepo);

        // Pass 2010 before 1010 intentionally -> service must still lock 1010 before 2010
        JournalEntry first = service.postEntryIdempotent(
                "idem-1", "=FORMULA_TEST", "DEPOSIT", "dep-1", "admin-1",
                List.of(
                        new LedgerDto.PostingItem("2010-ESCROW", PostingDirection.CREDIT, 1_500_000L),
                        new LedgerDto.PostingItem("1010-GATEWAY", PostingDirection.DEBIT, 1_500_000L)
                ));

        InOrder inOrder = inOrder(accountRepo);
        inOrder.verify(accountRepo).findByAccountNumberForUpdate("1010-GATEWAY");
        inOrder.verify(accountRepo).findByAccountNumberForUpdate("2010-ESCROW");

        // Second call with same idempotencyKey returns existing entry without mutating balances again
        when(journalRepo.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(first));
        JournalEntry replay = service.postEntryIdempotent(
                "idem-1", "=FORMULA_TEST", "DEPOSIT", "dep-1", "admin-1",
                List.of(
                        new LedgerDto.PostingItem("1010-GATEWAY", PostingDirection.DEBIT, 1_500_000L),
                        new LedgerDto.PostingItem("2010-ESCROW", PostingDirection.CREDIT, 1_500_000L)
                ));
        assertThat(replay.getId()).isEqualTo(first.getId());
        assertThat(gateway.getBalance()).isEqualTo(1_500_000L);
        assertThat(escrow.getBalance()).isEqualTo(1_500_000L);

        // CSV formula-injection neutralization check
        when(journalRepo.findAll()).thenReturn(List.of(first));
        when(accountRepo.findAll()).thenReturn(List.of(gateway, escrow));
        String glCsv = service.exportGeneralLedgerCsv(null, null);
        assertThat(glCsv).contains("\"'=FORMULA_TEST\"");
    }

    @Test
    void reverseEntryPostsBalancedCompensatingJournalEntry() {
        LedgerAccountRepository accountRepo = mock(LedgerAccountRepository.class);
        JournalEntryRepository journalRepo = mock(JournalEntryRepository.class);
        AlertNotificationPort alertPort = mock(AlertNotificationPort.class);
        ReconciliationReportRepository reconRepo = mock(ReconciliationReportRepository.class);

        LedgerAccount gateway = LedgerAccount.create("1010-GATEWAY", "Gateway", AccountType.ASSET, null);
        LedgerAccount escrow = LedgerAccount.create("2010-ESCROW", "Escrow", AccountType.LIABILITY, null);

        when(accountRepo.findByAccountNumberForUpdate("1010-GATEWAY")).thenReturn(Optional.of(gateway));
        when(accountRepo.findByAccountNumberForUpdate("2010-ESCROW")).thenReturn(Optional.of(escrow));
        when(accountRepo.findAll()).thenReturn(List.of(gateway, escrow));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(journalRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LedgerService service = new LedgerService(accountRepo, journalRepo, alertPort, reconRepo);
        JournalEntry posted = service.postEntry("Deposit", "DEPOSIT", "dep-9", "sys",
                List.of(
                        new LedgerDto.PostingItem("1010-GATEWAY", PostingDirection.DEBIT, 2_000_000L),
                        new LedgerDto.PostingItem("2010-ESCROW", PostingDirection.CREDIT, 2_000_000L)
                ));
        when(journalRepo.findById(posted.getId())).thenReturn(Optional.of(posted));

        JournalEntry reversed = service.reverseEntry(posted.getId(), "admin-1", "Duplicate deposit");
        assertThat(reversed.getReferenceType()).isEqualTo("REVERSAL");
        assertThat(gateway.getBalance()).isZero();
        assertThat(escrow.getBalance()).isZero();
    }
}

