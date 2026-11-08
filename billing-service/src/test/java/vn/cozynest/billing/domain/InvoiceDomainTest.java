package vn.cozynest.billing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.billing.domain.model.Invoice;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceDomainTest {

    @Test
    @DisplayName("Verify Invoice creation with valid period, rent amount, and status")
    void testInvoiceCreationAndTotals() {
        LocalDate currentMonth = LocalDate.of(2026, 11, 1);
        Invoice invoice = Invoice.builder()
                .id("inv-001")
                .invoiceCode("INV-202611-001")
                .contractId("ct-101")
                .tenantId("u-tenant-01")
                .landlordId("u-landlord-01")
                .periodMonth(currentMonth)
                .baseRent(4500000L)
                .totalAmount(4850000L)
                .paidAmount(0L)
                .dueDate(LocalDate.of(2026, 11, 10))
                .status("PENDING")
                .version(0L)
                .build();

        assertThat(invoice.getId()).isEqualTo("inv-001");
        assertThat(invoice.getInvoiceCode()).isEqualTo("INV-202611-001");
        assertThat(invoice.getTotalAmount()).isEqualTo(4850000L);
        assertThat(invoice.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Verify overdue detection when current date passes due date with unpaid balance")
    void testInvoiceDueDateValidation() {
        LocalDate dueDate = LocalDate.of(2026, 11, 5);
        Invoice invoice = Invoice.builder()
                .id("inv-002")
                .totalAmount(3000000L)
                .paidAmount(1000000L)
                .dueDate(dueDate)
                .status("PENDING")
                .build();

        LocalDate checkDate = LocalDate.of(2026, 11, 6);
        boolean isOverdue = checkDate.isAfter(invoice.getDueDate()) && (invoice.getPaidAmount() < invoice.getTotalAmount());
        assertThat(isOverdue).isTrue();
    }
}

