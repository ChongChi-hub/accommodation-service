package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.Invoice;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {
    Optional<Invoice> findById(String id);
    Optional<Invoice> findByInvoiceCode(String invoiceCode);
    List<Invoice> findByTenantId(String tenantId);
    List<Invoice> findByLandlordId(String landlordId);
    List<Invoice> findByContractId(String contractId);
    Invoice save(Invoice invoice);
}
