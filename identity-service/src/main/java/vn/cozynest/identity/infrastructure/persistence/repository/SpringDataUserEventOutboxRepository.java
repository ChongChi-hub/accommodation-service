package vn.cozynest.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.identity.infrastructure.persistence.entity.UserEventOutboxJpaEntity;

import java.util.List;

@Repository
public interface SpringDataUserEventOutboxRepository extends JpaRepository<UserEventOutboxJpaEntity, String> {
    List<UserEventOutboxJpaEntity> findTop50ByPublishedFalseOrderByCreatedAtAsc();
}

