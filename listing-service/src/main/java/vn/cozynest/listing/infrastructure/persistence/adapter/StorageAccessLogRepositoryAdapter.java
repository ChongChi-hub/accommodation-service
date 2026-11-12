package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.StorageAccessLog;
import vn.cozynest.listing.domain.repository.StorageAccessLogRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.StorageAccessLogJpaEntity;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataStorageAccessLogRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StorageAccessLogRepositoryAdapter implements StorageAccessLogRepository {

    private final SpringDataStorageAccessLogRepository springRepo;

    @Override
    public StorageAccessLog save(StorageAccessLog domain) {
        StorageAccessLogJpaEntity entity = StorageAccessLogJpaEntity.builder()
                .id(domain.getId())
                .actorId(domain.getActorId())
                .actorRole(domain.getActorRole())
                .bucket(domain.getBucket())
                .objectKey(domain.getObjectKey())
                .action(domain.getAction())
                .allowed(domain.isAllowed())
                .reason(domain.getReason())
                .createdAt(domain.getCreatedAt())
                .build();
        StorageAccessLogJpaEntity saved = springRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<StorageAccessLog> findByObjectKey(String objectKey) {
        return springRepo.findByObjectKeyOrderByCreatedAtDesc(objectKey).stream()
                .map(this::toDomain)
                .toList();
    }

    private StorageAccessLog toDomain(StorageAccessLogJpaEntity e) {
        return StorageAccessLog.builder()
                .id(e.getId())
                .actorId(e.getActorId())
                .actorRole(e.getActorRole())
                .bucket(e.getBucket())
                .objectKey(e.getObjectKey())
                .action(e.getAction())
                .allowed(e.isAllowed())
                .reason(e.getReason())
                .createdAt(e.getCreatedAt())
                .build();
    }
}

