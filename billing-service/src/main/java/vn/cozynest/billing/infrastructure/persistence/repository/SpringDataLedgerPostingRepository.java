package vn.cozynest.billing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.LedgerPostingJpaEntity;

import java.util.List;

public interface SpringDataLedgerPostingRepository extends JpaRepository<LedgerPostingJpaEntity, String> {
    List<LedgerPostingJpaEntity> findByJournalEntryId(String journalEntryId);
}

