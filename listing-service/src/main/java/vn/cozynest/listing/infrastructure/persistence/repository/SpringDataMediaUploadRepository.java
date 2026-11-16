package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.infrastructure.persistence.entity.MediaUploadJpaEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataMediaUploadRepository extends JpaRepository<MediaUploadJpaEntity, String> {

    Optional<MediaUploadJpaEntity> findByObjectKey(String objectKey);

    @Query("""
            SELECT m FROM MediaUploadJpaEntity m
            WHERE (m.status = 'PENDING_UPLOAD' OR (m.status = 'CONFIRMED' AND m.attachedEntityId IS NULL))
              AND m.createdAt < :cutoff
            ORDER BY m.createdAt ASC
            """)
    List<MediaUploadJpaEntity> findOrphanUploads(@Param("cutoff") Instant cutoff);

    long countByStatus(String status);
}
