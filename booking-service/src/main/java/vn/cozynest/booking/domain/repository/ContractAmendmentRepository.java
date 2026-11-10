package vn.cozynest.booking.domain.repository;

import vn.cozynest.booking.domain.model.ContractAmendment;

import java.util.List;
import java.util.Optional;

public interface ContractAmendmentRepository {
    ContractAmendment save(ContractAmendment amendment);
    Optional<ContractAmendment> findById(String id);
    List<ContractAmendment> findByContractId(String contractId);
}

