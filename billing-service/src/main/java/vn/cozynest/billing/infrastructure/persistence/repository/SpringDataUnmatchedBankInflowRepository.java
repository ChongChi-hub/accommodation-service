package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.billing.infrastructure.persistence.entity.UnmatchedBankInflowJpaEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataUnmatchedBankInflowRepository extends JpaRepository<UnmatchedBankInflowJpaEntity, String> {
    boolean existsByProviderTxId(String providerTxId);
    Optional<UnmatchedBankInflowJpaEntity> findByProviderTxId(String providerTxId);
    List<UnmatchedBankInflowJpaEntity> findByStatusOrderByReceivedAtDesc(String status);
    List<UnmatchedBankInflowJpaEntity> findAllByOrderByReceivedAtDesc();
}

