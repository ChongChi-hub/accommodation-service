package vn.toam.booking.application.port;

import vn.toam.booking.domain.model.Contract;
import java.util.Optional;

public interface ContractQueryPort {
    Optional<Contract> getContractById(String id);
}
