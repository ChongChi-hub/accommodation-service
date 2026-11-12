package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.JournalEntry;

import java.util.List;
import java.util.Optional;

public interface JournalEntryRepository {
    JournalEntry save(JournalEntry entry);
    Optional<JournalEntry> findById(String id);
    Optional<JournalEntry> findByEntryNumber(String entryNumber);
    default Optional<JournalEntry> findByIdempotencyKey(String idempotencyKey) { return Optional.empty(); }
    List<JournalEntry> findByReference(String referenceType, String referenceId);
    List<JournalEntry> findAll();
}
