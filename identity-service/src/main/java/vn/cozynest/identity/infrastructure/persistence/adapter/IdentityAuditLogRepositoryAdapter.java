package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.IdentityAuditLog;
import vn.cozynest.identity.domain.repository.IdentityAuditLogRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.IdentityAuditLogJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataIdentityAuditLogRepository;

import java.util.List;

@Component
public class IdentityAuditLogRepositoryAdapter implements IdentityAuditLogRepository {

    private final SpringDataIdentityAuditLogRepository repository;

    public IdentityAuditLogRepositoryAdapter(SpringDataIdentityAuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public IdentityAuditLog save(IdentityAuditLog log) {
        IdentityAuditLogJpaEntity entity = IdentityAuditLogJpaEntity.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .actorId(log.getActorId())
                .eventType(log.getEventType())
                .provider(log.getProvider())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
        IdentityAuditLogJpaEntity saved = repository.save(entity);
        return new IdentityAuditLog(
                saved.getId(), saved.getUserId(), saved.getActorId(),
                saved.getEventType(), saved.getProvider(), saved.getIpAddress(),
                saved.getUserAgent(), saved.getDetails(), saved.getCreatedAt()
        );
    }

    @Override
    public List<IdentityAuditLog> findByUserIdOrderByCreatedAtDesc(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(e -> new IdentityAuditLog(
                        e.getId(), e.getUserId(), e.getActorId(),
                        e.getEventType(), e.getProvider(), e.getIpAddress(),
                        e.getUserAgent(), e.getDetails(), e.getCreatedAt()))
                .toList();
    }
}
