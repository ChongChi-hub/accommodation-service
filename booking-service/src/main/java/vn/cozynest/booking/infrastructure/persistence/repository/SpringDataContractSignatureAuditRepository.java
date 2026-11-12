package vn.cozynest.booking.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.booking.infrastructure.persistence.entity.ContractSignatureAuditJpaEntity;

import java.util.List;

@Repository
public interface SpringDataContractSignatureAuditRepository extends JpaRepository<ContractSignatureAuditJpaEntity, String> {
    List<ContractSignatureAuditJpaEntity> findByContractIdOrderBySignedAtAsc(String contractId);
}

