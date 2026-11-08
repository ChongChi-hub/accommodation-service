package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.cozynest.identity.infrastructure.persistence.entity.FaceEventOutboxJpaEntity;

import java.util.List;

@Repository
public interface SpringDataFaceEventOutboxRepository extends JpaRepository<FaceEventOutboxJpaEntity, String> {

    @Query("SELECT o FROM FaceEventOutboxJpaEntity o WHERE o.published = false ORDER BY o.createdAt ASC")
    List<FaceEventOutboxJpaEntity> findUnpublished(Pageable pageable);
}

