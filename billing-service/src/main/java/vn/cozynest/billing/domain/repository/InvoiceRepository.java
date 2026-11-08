package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.Invoice;
import java.util.Optional;

public interface InvoiceRepository {
    Optional<Invoice> findById(String id);
    Optional<Invoice> findByInvoiceCode(String invoiceCode);
    Invoice save(Invoice invoice);
}
