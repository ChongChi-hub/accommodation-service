package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.cozynest.listing.domain.model.MediaUpload;
import vn.cozynest.listing.domain.repository.MediaUploadRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.MediaUploadJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataMediaUploadRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MediaUploadRepositoryAdapter implements MediaUploadRepository {

    private final SpringDataMediaUploadRepository springDataRepository;

    @Override
    public MediaUpload save(MediaUpload upload) {
        MediaUploadJpaEntity entity = toEntity(upload);
        MediaUploadJpaEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<MediaUpload> findById(String id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<MediaUpload> findByObjectKey(String objectKey) {
        return springDataRepository.findByObjectKey(objectKey).map(this::toDomain);
    }

    @Override
    public List<MediaUpload> findOrphanUploads(Instant cutoff) {
        return springDataRepository.findOrphanUploads(cutoff).stream().map(this::toDomain).toList();
    }

    @Override
    public long countByStatus(String status) {
        return springDataRepository.countByStatus(status);
    }

    private MediaUpload toDomain(MediaUploadJpaEntity e) {
        return new MediaUpload(
                e.getId(), e.getObjectKey(), e.getBucket(), e.getCategory(), e.getContentType(),
                e.getSizeBytes(), e.getStatus(), e.getUploaderId(), e.getAttachedEntityType(),
                e.getAttachedEntityId(), e.getCreatedAt(), e.getConfirmedAt(), e.getAttachedAt()
        );
    }

    private MediaUploadJpaEntity toEntity(MediaUpload d) {
        return MediaUploadJpaEntity.builder()
                .id(d.getId())
                .objectKey(d.getObjectKey())
                .bucket(d.getBucket())
                .category(d.getCategory())
                .contentType(d.getContentType())
                .sizeBytes(d.getSizeBytes())
                .status(d.getStatus())
                .uploaderId(d.getUploaderId())
                .attachedEntityType(d.getAttachedEntityType())
                .attachedEntityId(d.getAttachedEntityId())
                .createdAt(d.getCreatedAt())
                .confirmedAt(d.getConfirmedAt())
                .attachedAt(d.getAttachedAt())
                .build();
    }
}
