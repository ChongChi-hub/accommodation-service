package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.ReconciliationReportJpaEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataReconciliationReportRepository extends JpaRepository<ReconciliationReportJpaEntity, String> {
    Optional<ReconciliationReportJpaEntity> findByReconDate(LocalDate reconDate);
    List<ReconciliationReportJpaEntity> findTop30ByOrderByReconDateDesc();
}

