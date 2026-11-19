package vn.cozynest.billing.application.service;

import org.junit.jupiter.api.Test;
import vn.cozynest.billing.application.exception.WebhookSecurityException;
import vn.cozynest.billing.domain.model.BillingEventOutbox;
import vn.cozynest.billing.domain.model.Invoice;
import vn.cozynest.billing.domain.repository.BillingEventOutboxRepository;
import vn.cozynest.billing.domain.repository.InvoiceRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvoiceIdorAndReminderTest {

    @Test
    void returns404OnCrossTenantInvoiceReadAndRateLimitsManualReminder() {
        InvoiceRepository repo = mock(InvoiceRepository.class);
        BillingEventOutboxRepository outboxRepo = mock(BillingEventOutboxRepository.class);
        InvoiceService service = new InvoiceService(
                repo, mock(LedgerService.class), Optional.empty(),
                Optional.empty(), Optional.of(outboxRepo), Optional.empty()
        );

        Invoice inv = Invoice.create(
                "INV-202611-001", "ct-1", null, "u-tenant-01", "u-landlord-01",
                LocalDate.of(2026, 11, 1), 4_000_000L, 0L, 0L, 0L, 0L, LocalDate.of(2026, 11, 5)
        );
        when(repo.findById(inv.getId())).thenReturn(Optional.of(inv));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        // IDOR: outsider gets 404
        assertThatThrownBy(() -> service.getInvoiceById(inv.getId(), "u-tenant-02", false))
                .isInstanceOf(WebhookSecurityException.class)
                .extracting(e -> ((WebhookSecurityException) e).getHttpStatus())
                .isEqualTo(404);

        // Landlord sends first manual reminder -> succeeds & emits outbox event
        service.sendManualReminder(inv.getId(), "u-landlord-01", false);
        assertThat(inv.getReminderCount()).isEqualTo(1);
        verify(outboxRepo, times(1)).save(argThat((BillingEventOutbox e) -> "INVOICE_OVERDUE_REMINDER".equals(e.getEventType())));

        // Second reminder within 24h -> rejected (409)
        assertThatThrownBy(() -> service.sendManualReminder(inv.getId(), "u-landlord-01", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("24");
    }
}

