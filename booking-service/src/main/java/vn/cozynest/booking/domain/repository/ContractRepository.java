package vn.cozynest.booking.domain.repository;

import vn.cozynest.booking.domain.model.Contract;
import java.util.Optional;

public interface ContractRepository {
    Optional<Contract> findById(String id);
    Optional<Contract> findByContractCode(String contractCode);
    Contract save(Contract contract);
}
