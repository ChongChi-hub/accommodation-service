package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.UtilityReadingAudit;
import vn.cozynest.billing.domain.repository.UtilityReadingAuditRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.UtilityReadingAuditJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataUtilityReadingAuditRepository;

import java.util.List;

@Component
public class UtilityReadingAuditRepositoryAdapter implements UtilityReadingAuditRepository {

    private final SpringDataUtilityReadingAuditRepository repository;

    public UtilityReadingAuditRepositoryAdapter(SpringDataUtilityReadingAuditRepository repository) {
        this.repository = repository;
    }

    @Override
    public UtilityReadingAudit save(UtilityReadingAudit audit) {
        UtilityReadingAuditJpaEntity saved = repository.save(UtilityReadingAuditJpaEntity.builder()
                .id(audit.id())
                .readingId(audit.readingId())
                .action(audit.action())
                .prevSnapshotJson(audit.prevSnapshotJson())
                .actorId(audit.actorId())
                .reason(audit.reason())
                .createdAt(audit.createdAt())
                .build());
        return toDomain(saved);
    }

    @Override
    public List<UtilityReadingAudit> findByReadingId(String readingId) {
        return repository.findByReadingIdOrderByCreatedAtDesc(readingId).stream().map(this::toDomain).toList();
    }

    private UtilityReadingAudit toDomain(UtilityReadingAuditJpaEntity e) {
        return new UtilityReadingAudit(e.getId(), e.getReadingId(), e.getAction(),
                e.getPrevSnapshotJson(), e.getActorId(), e.getReason(), e.getCreatedAt());
    }
}

