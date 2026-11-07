package vn.toam.booking.domain.repository;

import vn.toam.booking.domain.model.Contract;
import java.util.Optional;

public interface ContractRepository {
    Optional<Contract> findById(String id);
    Optional<Contract> findByContractCode(String contractCode);
    Contract save(Contract contract);
}
