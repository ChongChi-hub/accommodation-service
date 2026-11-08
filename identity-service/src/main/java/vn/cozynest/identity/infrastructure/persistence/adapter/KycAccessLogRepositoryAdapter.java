package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.KycAccessLog;
import vn.cozynest.identity.domain.repository.KycAccessLogRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.KycAccessLogJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataKycAccessLogRepository;

import java.util.List;

@Component
public class KycAccessLogRepositoryAdapter implements KycAccessLogRepository {

    private final SpringDataKycAccessLogRepository springRepo;

    public KycAccessLogRepositoryAdapter(SpringDataKycAccessLogRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public List<KycAccessLog> findByVerificationIdOrderByCreatedAtDesc(String verificationId) {
        return springRepo.findByVerificationIdOrderByCreatedAtDesc(verificationId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public KycAccessLog save(KycAccessLog log) {
        KycAccessLogJpaEntity entity = toEntity(log);
        KycAccessLogJpaEntity saved = springRepo.save(entity);
        return toDomain(saved);
    }

    private KycAccessLog toDomain(KycAccessLogJpaEntity entity) {
        return new KycAccessLog(
                entity.getId(),
                entity.getVerificationId(),
                entity.getActorId(),
                entity.getActorRole(),
                entity.getAction(),
                entity.getIp(),
                entity.getCreatedAt()
        );
    }

    private KycAccessLogJpaEntity toEntity(KycAccessLog domain) {
        KycAccessLogJpaEntity entity = new KycAccessLogJpaEntity();
        entity.setId(domain.getId());
        entity.setVerificationId(domain.getVerificationId());
        entity.setActorId(domain.getActorId());
        entity.setActorRole(domain.getActorRole());
        entity.setAction(domain.getAction());
        entity.setIp(domain.getIp());
        return entity;
    }
}

