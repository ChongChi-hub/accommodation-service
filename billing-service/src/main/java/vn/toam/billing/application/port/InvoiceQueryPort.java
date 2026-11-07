package vn.toam.billing.application.port;

import vn.toam.billing.domain.model.Invoice;
import java.util.Optional;

public interface InvoiceQueryPort {
    Optional<Invoice> getInvoiceById(String id);
}
