package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.KycStatusHistory;
import vn.cozynest.identity.domain.repository.KycStatusHistoryRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.KycStatusHistoryJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataKycStatusHistoryRepository;

import java.util.List;

@Component
public class KycStatusHistoryRepositoryAdapter implements KycStatusHistoryRepository {

    private final SpringDataKycStatusHistoryRepository springRepo;

    public KycStatusHistoryRepositoryAdapter(SpringDataKycStatusHistoryRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public List<KycStatusHistory> findByVerificationIdOrderByCreatedAtAsc(String verificationId) {
        return springRepo.findByVerificationIdOrderByCreatedAtAsc(verificationId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public KycStatusHistory save(KycStatusHistory history) {
        KycStatusHistoryJpaEntity entity = toEntity(history);
        KycStatusHistoryJpaEntity saved = springRepo.save(entity);
        return toDomain(saved);
    }

    private KycStatusHistory toDomain(KycStatusHistoryJpaEntity entity) {
        return new KycStatusHistory(
                entity.getId(),
                entity.getVerificationId(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getActorId(),
                entity.getReason(),
                entity.getCreatedAt()
        );
    }

    private KycStatusHistoryJpaEntity toEntity(KycStatusHistory domain) {
        KycStatusHistoryJpaEntity entity = new KycStatusHistoryJpaEntity();
        entity.setId(domain.getId());
        entity.setVerificationId(domain.getVerificationId());
        entity.setFromStatus(domain.getFromStatus());
        entity.setToStatus(domain.getToStatus());
        entity.setActorId(domain.getActorId());
        entity.setReason(domain.getReason());
        return entity;
    }
}

