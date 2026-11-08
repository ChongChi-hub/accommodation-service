package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.FaceAccessLog;
import vn.cozynest.identity.domain.repository.FaceAccessLogRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.FaceAccessLogJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataFaceAccessLogRepository;

import java.util.List;

@Component
public class FaceAccessLogRepositoryAdapter implements FaceAccessLogRepository {

    private final SpringDataFaceAccessLogRepository repository;

    public FaceAccessLogRepositoryAdapter(SpringDataFaceAccessLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public FaceAccessLog save(FaceAccessLog log) {
        FaceAccessLogJpaEntity entity = new FaceAccessLogJpaEntity();
        entity.setId(log.getId());
        entity.setActorId(log.getActorId());
        entity.setActorRole(log.getActorRole());
        entity.setFrameId(log.getFrameId());
        entity.setFaceVerificationId(log.getFaceVerificationId());
        entity.setPurpose(log.getPurpose());
        entity.setIpAddress(log.getIpAddress());
        entity.setUserAgent(log.getUserAgent());
        FaceAccessLogJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<FaceAccessLog> findByFaceVerificationId(String faceVerificationId) {
        return repository.findByFaceVerificationIdOrderByCreatedAtDesc(faceVerificationId).stream()
                .map(this::toDomain)
                .toList();
    }

    private FaceAccessLog toDomain(FaceAccessLogJpaEntity e) {
        return new FaceAccessLog(
                e.getId(), e.getActorId(), e.getActorRole(), e.getFrameId(),
                e.getFaceVerificationId(), e.getPurpose(), e.getIpAddress(),
                e.getUserAgent(), e.getCreatedAt()
        );
    }
}

