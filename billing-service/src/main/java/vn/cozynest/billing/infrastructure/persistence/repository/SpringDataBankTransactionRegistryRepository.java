package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.billing.infrastructure.persistence.entity.BankTransactionRecordJpaEntity;

import java.util.Optional;

@Repository
public interface SpringDataBankTransactionRegistryRepository extends JpaRepository<BankTransactionRecordJpaEntity, String> {
    boolean existsByNormalizedTxCode(String normalizedTxCode);
    Optional<BankTransactionRecordJpaEntity> findByNormalizedTxCode(String normalizedTxCode);
}

