package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.ReconciliationReport;
import vn.cozynest.billing.domain.repository.ReconciliationReportRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.ReconciliationReportJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataReconciliationReportRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class ReconciliationReportRepositoryAdapter implements ReconciliationReportRepository {

    private final SpringDataReconciliationReportRepository repository;

    public ReconciliationReportRepositoryAdapter(SpringDataReconciliationReportRepository repository) {
        this.repository = repository;
    }

    @Override
    public ReconciliationReport save(ReconciliationReport domain) {
        ReconciliationReportJpaEntity entity = toEntity(domain);
        ReconciliationReportJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ReconciliationReport> findByReconDate(LocalDate reconDate) {
        return repository.findByReconDate(reconDate).map(this::toDomain);
    }

    @Override
    public List<ReconciliationReport> findRecentReports(int limit) {
        return repository.findTop30ByOrderByReconDateDesc().stream().map(this::toDomain).toList();
    }

    private ReconciliationReport toDomain(ReconciliationReportJpaEntity entity) {
        return new ReconciliationReport(
                entity.getId(),
                entity.getReconDate(),
                entity.getTotalInternalTx(),
                entity.getTotalGatewayTx(),
                entity.getMatchedTx(),
                entity.getMismatchedTx(),
                entity.getStatus(),
                entity.getSummaryNotes(),
                entity.getCreatedAt()
        );
    }

    private ReconciliationReportJpaEntity toEntity(ReconciliationReport domain) {
        return ReconciliationReportJpaEntity.builder()
                .id(domain.getId())
                .reconDate(domain.getReconDate())
                .totalInternalTx(domain.getTotalInternalTx())
                .totalGatewayTx(domain.getTotalGatewayTx())
                .matchedTx(domain.getMatchedTx())
                .mismatchedTx(domain.getMismatchedTx())
                .status(domain.getStatus())
                .summaryNotes(domain.getSummaryNotes())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

