package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.UtilityReading;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UtilityReadingRepository {
    UtilityReading save(UtilityReading reading);
    Optional<UtilityReading> findById(String id);
    Optional<UtilityReading> findByContractIdAndPeriodMonth(String contractId, LocalDate periodMonth);
    Optional<UtilityReading> findLatestByContractId(String contractId);
    List<UtilityReading> findByContractId(String contractId);
}

