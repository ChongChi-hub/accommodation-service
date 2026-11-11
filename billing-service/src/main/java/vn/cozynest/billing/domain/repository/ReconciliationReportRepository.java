package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.ReconciliationReport;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReconciliationReportRepository {
    ReconciliationReport save(ReconciliationReport report);
    Optional<ReconciliationReport> findByReconDate(LocalDate reconDate);
    List<ReconciliationReport> findRecentReports(int limit);
}

