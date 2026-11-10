package vn.cozynest.booking.domain.repository;

import vn.cozynest.booking.domain.model.ContractOccupant;

import java.util.List;
import java.util.Optional;

public interface ContractOccupantRepository {
    ContractOccupant save(ContractOccupant occupant);
    Optional<ContractOccupant> findById(String id);
    List<ContractOccupant> findByContractId(String contractId);
    void deleteById(String id);
}

