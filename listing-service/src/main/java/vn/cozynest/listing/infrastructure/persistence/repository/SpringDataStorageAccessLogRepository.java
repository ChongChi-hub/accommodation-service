package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.StorageAccessLogJpaEntity;

import java.util.List;

public interface SpringDataStorageAccessLogRepository extends JpaRepository<StorageAccessLogJpaEntity, String> {

    List<StorageAccessLogJpaEntity> findByObjectKeyOrderByCreatedAtDesc(String objectKey);
}

