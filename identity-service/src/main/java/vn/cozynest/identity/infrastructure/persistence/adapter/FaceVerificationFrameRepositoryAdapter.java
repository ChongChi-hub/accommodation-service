package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.FaceVerificationFrame;
import vn.cozynest.identity.domain.repository.FaceVerificationFrameRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.FaceVerificationFrameJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataFaceVerificationFrameRepository;

import java.util.List;
import java.util.Optional;

@Component
public class FaceVerificationFrameRepositoryAdapter implements FaceVerificationFrameRepository {

    private final SpringDataFaceVerificationFrameRepository repository;

    public FaceVerificationFrameRepositoryAdapter(SpringDataFaceVerificationFrameRepository repository) {
        this.repository = repository;
    }

    @Override
    public FaceVerificationFrame save(FaceVerificationFrame frame) {
        return toDomain(repository.save(toEntity(frame)));
    }

    @Override
    public List<FaceVerificationFrame> saveAll(List<FaceVerificationFrame> frames) {
        List<FaceVerificationFrameJpaEntity> entities = frames.stream().map(this::toEntity).toList();
        return repository.saveAll(entities).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<FaceVerificationFrame> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<FaceVerificationFrame> findByFaceVerificationId(String faceVerificationId) {
        return repository.findByFaceVerificationId(faceVerificationId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<FaceVerificationFrame> findActiveByFaceVerificationId(String faceVerificationId) {
        return repository.findActiveByFaceVerificationId(faceVerificationId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteByFaceVerificationId(String faceVerificationId) {
        repository.deleteByFaceVerificationId(faceVerificationId);
    }

    private FaceVerificationFrame toDomain(FaceVerificationFrameJpaEntity e) {
        return new FaceVerificationFrame(
                e.getId(), e.getFaceVerificationId(), e.getPurpose(),
                e.getStorageKey(), e.getChecksum(), e.getSize(),
                e.getCreatedAt(), e.getDeletedAt()
        );
    }

    private FaceVerificationFrameJpaEntity toEntity(FaceVerificationFrame d) {
        FaceVerificationFrameJpaEntity e = new FaceVerificationFrameJpaEntity();
        e.setId(d.getId());
        e.setFaceVerificationId(d.getFaceVerificationId());
        e.setPurpose(d.getPurpose());
        e.setStorageKey(d.getStorageKey());
        e.setChecksum(d.getChecksum());
        e.setSize(d.getSize());
        e.setDeletedAt(d.getDeletedAt());
        return e;
    }
}

