package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.cozynest.billing.infrastructure.persistence.entity.InvoiceJpaEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataInvoiceRepository extends JpaRepository<InvoiceJpaEntity, String> {
    Optional<InvoiceJpaEntity> findByInvoiceCode(String invoiceCode);
    List<InvoiceJpaEntity> findByTenantIdOrderByPeriodMonthDesc(String tenantId);
    List<InvoiceJpaEntity> findByLandlordIdOrderByPeriodMonthDesc(String landlordId);
    List<InvoiceJpaEntity> findByContractIdOrderByPeriodMonthDesc(String contractId);
    List<InvoiceJpaEntity> findByUtilityReadingId(String utilityReadingId);
    List<InvoiceJpaEntity> findByContractIdAndPeriodMonth(String contractId, LocalDate periodMonth);

    @Query("SELECT i FROM InvoiceJpaEntity i WHERE i.dueDate < :today AND i.status IN ('ISSUED', 'PARTIALLY_PAID')")
    List<InvoiceJpaEntity> findOverdueCandidates(@Param("today") LocalDate today);
}
