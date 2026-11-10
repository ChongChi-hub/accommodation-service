package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.JournalEntryJpaEntity;

import java.util.List;
import java.util.Optional;

public interface SpringDataJournalEntryRepository extends JpaRepository<JournalEntryJpaEntity, String> {
    Optional<JournalEntryJpaEntity> findByEntryNumber(String entryNumber);
    List<JournalEntryJpaEntity> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);
}

