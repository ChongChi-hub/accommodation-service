package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.ReviewJpaEntity;

import java.util.List;

public interface SpringDataReviewRepository extends JpaRepository<ReviewJpaEntity, Long> {
    List<ReviewJpaEntity> findByRoomIdOrderByCreatedAtDesc(Long roomId);
    List<ReviewJpaEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    List<ReviewJpaEntity> findByRoomIdInOrderByCreatedAtDesc(List<Long> roomIds);
}

