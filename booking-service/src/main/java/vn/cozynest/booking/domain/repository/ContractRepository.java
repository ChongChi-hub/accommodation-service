package vn.cozynest.booking.domain.repository;

import vn.cozynest.booking.domain.model.Contract;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContractRepository {
    Optional<Contract> findById(String id);
    Optional<Contract> findByContractCode(String contractCode);
    List<Contract> findByTenantId(String tenantId);
    List<Contract> findByLandlordId(String landlordId);
    Optional<Contract> findActiveByRoomId(String roomId);
    List<Contract> findExpiringBetween(LocalDate start, LocalDate end);
    Contract save(Contract contract);
}
