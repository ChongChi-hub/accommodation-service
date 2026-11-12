package vn.cozynest.booking.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.booking.infrastructure.persistence.entity.ContractOccupantJpaEntity;

import java.util.List;

@Repository
public interface SpringDataContractOccupantRepository extends JpaRepository<ContractOccupantJpaEntity, String> {
    List<ContractOccupantJpaEntity> findByContractId(String contractId);

    List<ContractOccupantJpaEntity> findByContractIdAndRemovedAtIsNull(String contractId);
}

