package vn.cozynest.booking.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.booking.infrastructure.persistence.entity.ContractAmendmentJpaEntity;

import java.util.List;

@Repository
public interface SpringDataContractAmendmentRepository extends JpaRepository<ContractAmendmentJpaEntity, String> {
    List<ContractAmendmentJpaEntity> findByContractId(String contractId);
}

