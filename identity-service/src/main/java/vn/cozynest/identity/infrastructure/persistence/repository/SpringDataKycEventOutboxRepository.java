package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.identity.infrastructure.persistence.entity.KycEventOutboxJpaEntity;

import java.util.List;

@Repository
public interface SpringDataKycEventOutboxRepository extends JpaRepository<KycEventOutboxJpaEntity, String> {

    List<KycEventOutboxJpaEntity> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
}

