package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.infrastructure.persistence.entity.OwnershipDocAccessLogJpaEntity;

import java.util.List;

@Repository
public interface SpringDataOwnershipDocAccessLogRepository extends JpaRepository<OwnershipDocAccessLogJpaEntity, String> {
    List<OwnershipDocAccessLogJpaEntity> findByRoomIdOrderByAccessedAtDesc(Long roomId);
}

