package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.EscrowHoldJpaEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SpringDataEscrowHoldRepository extends JpaRepository<EscrowHoldJpaEntity, String> {
    Optional<EscrowHoldJpaEntity> findByContractId(String contractId);
    List<EscrowHoldJpaEntity> findByStatusAndInspectionEndsAtBefore(String status, Instant cutoff);
    List<EscrowHoldJpaEntity> findByTenantId(String tenantId);
    List<EscrowHoldJpaEntity> findByLandlordId(String landlordId);
    boolean existsByBankTxCode(String bankTxCode);
    boolean existsByReceiptHash(String receiptHash);
}

