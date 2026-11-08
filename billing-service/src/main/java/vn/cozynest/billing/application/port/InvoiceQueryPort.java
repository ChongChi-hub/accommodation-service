package vn.cozynest.billing.application.port;

import vn.cozynest.billing.domain.model.Invoice;
import java.util.Optional;

public interface InvoiceQueryPort {
    Optional<Invoice> getInvoiceById(String id);
}
