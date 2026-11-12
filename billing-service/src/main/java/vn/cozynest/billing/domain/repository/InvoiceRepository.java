package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.Invoice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {
    Optional<Invoice> findById(String id);
    Optional<Invoice> findByInvoiceCode(String invoiceCode);
    List<Invoice> findByTenantId(String tenantId);
    List<Invoice> findByLandlordId(String landlordId);
    List<Invoice> findByContractId(String contractId);
    List<Invoice> findByUtilityReadingId(String utilityReadingId);
    List<Invoice> findByContractIdAndPeriodMonth(String contractId, LocalDate periodMonth);
    List<Invoice> findOverdueCandidates(LocalDate today);
    Invoice save(Invoice invoice);
}
