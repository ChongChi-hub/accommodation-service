package vn.cozynest.booking.application.port;

import vn.cozynest.booking.domain.model.Contract;
import java.util.Optional;

public interface ContractQueryPort {
    Optional<Contract> getContractById(String id);
}
