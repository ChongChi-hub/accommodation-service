package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.UtilityReadingJpaEntity;

import java.time.LocalDate;
import java.util.Optional;

public interface SpringDataUtilityReadingRepository extends JpaRepository<UtilityReadingJpaEntity, String> {
    Optional<UtilityReadingJpaEntity> findByContractIdAndPeriodMonth(String contractId, LocalDate periodMonth);
    Optional<UtilityReadingJpaEntity> findFirstByContractIdOrderByPeriodMonthDesc(String contractId);
}

