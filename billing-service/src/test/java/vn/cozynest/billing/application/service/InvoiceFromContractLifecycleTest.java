package vn.cozynest.billing.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.billing.application.dto.InvoiceDto;
import vn.cozynest.billing.application.port.BillingContractPort;
import vn.cozynest.billing.domain.model.Invoice;
import vn.cozynest.billing.domain.repository.InvoiceRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvoiceFromContractLifecycleTest {

    @Test
    @DisplayName("BUG-04: GenerateFromContract defaults null optional fees to 0, enforces ACTIVE contract, and deduplicates same period")
    void generatesAndDeduplicatesMonthlyInvoiceFromActiveContract() {
        InvoiceRepository repo = mock(InvoiceRepository.class);
        BillingContractPort contractPort = mock(BillingContractPort.class);

        List<Invoice> stored = new ArrayList<>();
        when(repo.findByContractIdAndPeriodMonth(eq("ct-1"), any())).thenAnswer(inv -> List.copyOf(stored));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            stored.add(i);
            return i;
        });

        when(contractPort.getContractSnapshot("ct-1", LocalDate.of(2026, 12, 1)))
                .thenReturn(Optional.of(new BillingContractPort.ContractSnapshot(
                        "ct-1", "10", "u-tenant-01", "u-landlord-01", "ACTIVE", 5500000L, 5
                )));

        InvoiceService svc = new InvoiceService(repo, null, Optional.of(contractPort), Optional.empty(), Optional.empty(), Optional.empty());

        InvoiceDto.GenerateFromContractCommand cmd = new InvoiceDto.GenerateFromContractCommand(
                "ct-1", LocalDate.of(2026, 12, 1), null, null
        );
        assertThat(cmd.serviceAmount()).isEqualTo(0L);
        assertThat(cmd.otherAmount()).isEqualTo(0L);

        InvoiceDto.InvoiceResponse first = svc.generateFromContract(cmd, "u-landlord-01", false);
        InvoiceDto.InvoiceResponse second = svc.generateFromContract(cmd, "u-landlord-01", false);

        assertThat(first.id()).isEqualTo(second.id());
        assertThat(first.totalAmount()).isEqualTo(5500000L);
        verify(repo, times(1)).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Rejects generating invoice for non-ACTIVE contract")
    void rejectsNonActiveContract() {
        InvoiceRepository repo = mock(InvoiceRepository.class);
        BillingContractPort contractPort = mock(BillingContractPort.class);
        when(contractPort.getContractSnapshot("ct-draft", LocalDate.of(2026, 12, 1)))
                .thenReturn(Optional.of(new BillingContractPort.ContractSnapshot(
                        "ct-draft", "10", "u-tenant-01", "u-landlord-01", "PENDING_SIGN", 5500000L, 5
                )));

        InvoiceService svc = new InvoiceService(repo, null, Optional.of(contractPort), Optional.empty(), Optional.empty(), Optional.empty());

        assertThatThrownBy(() -> svc.generateFromContract(
                new InvoiceDto.GenerateFromContractCommand("ct-draft", LocalDate.of(2026, 12, 1), 0L, 0L),
                "u-landlord-01", false
        )).isInstanceOf(IllegalStateException.class).hasMessageContaining("PENDING_SIGN");
    }
}

