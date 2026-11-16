package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.infrastructure.persistence.entity.ListingEventOutboxJpaEntity;

import java.util.List;

@Repository
public interface SpringDataListingEventOutboxRepository extends JpaRepository<ListingEventOutboxJpaEntity, String> {

    @Query("SELECT e FROM ListingEventOutboxJpaEntity e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    List<ListingEventOutboxJpaEntity> findPending(Pageable pageable);
}
