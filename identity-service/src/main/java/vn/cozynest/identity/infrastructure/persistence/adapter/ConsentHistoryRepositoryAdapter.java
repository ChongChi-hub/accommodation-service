package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.ConsentHistory;
import vn.cozynest.identity.domain.repository.ConsentHistoryRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.ConsentHistoryJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataConsentHistoryRepository;

import java.util.List;

@Component
public class ConsentHistoryRepositoryAdapter implements ConsentHistoryRepository {

    private final SpringDataConsentHistoryRepository springRepo;

    public ConsentHistoryRepositoryAdapter(SpringDataConsentHistoryRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public ConsentHistory save(ConsentHistory consent) {
        ConsentHistoryJpaEntity entity = toEntity(consent);
        ConsentHistoryJpaEntity saved = springRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<ConsentHistory> findByUserIdOrderByCreatedAtDesc(String userId) {
        return springRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    private ConsentHistory toDomain(ConsentHistoryJpaEntity entity) {
        return new ConsentHistory(
                entity.getId(),
                entity.getUserId(),
                entity.getConsentType(),
                entity.getVersion(),
                entity.isAgreed(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getMetadataJson(),
                entity.getCreatedAt()
        );
    }

    private ConsentHistoryJpaEntity toEntity(ConsentHistory domain) {
        return ConsentHistoryJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .consentType(domain.getConsentType())
                .version(domain.getVersion())
                .agreed(domain.isAgreed())
                .ipAddress(domain.getIpAddress())
                .userAgent(domain.getUserAgent())
                .metadataJson(domain.getMetadataJson())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
