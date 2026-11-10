package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.InvoiceJpaEntity;

import java.util.List;
import java.util.Optional;

public interface SpringDataInvoiceRepository extends JpaRepository<InvoiceJpaEntity, String> {
    Optional<InvoiceJpaEntity> findByInvoiceCode(String invoiceCode);
    List<InvoiceJpaEntity> findByTenantIdOrderByPeriodMonthDesc(String tenantId);
    List<InvoiceJpaEntity> findByLandlordIdOrderByPeriodMonthDesc(String landlordId);
    List<InvoiceJpaEntity> findByContractIdOrderByPeriodMonthDesc(String contractId);
}

