package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.JournalEntry;
import vn.cozynest.billing.domain.model.LedgerPosting;
import vn.cozynest.billing.domain.model.PostingDirection;
import vn.cozynest.billing.domain.repository.JournalEntryRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.JournalEntryJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.entity.LedgerAccountJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.entity.LedgerPostingJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataJournalEntryRepository;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataLedgerAccountRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class JournalEntryRepositoryAdapter implements JournalEntryRepository {
    private final SpringDataJournalEntryRepository repository;
    private final SpringDataLedgerAccountRepository accountRepository;

    public JournalEntryRepositoryAdapter(SpringDataJournalEntryRepository repository,
                                         SpringDataLedgerAccountRepository accountRepository) {
        this.repository = repository;
        this.accountRepository = accountRepository;
    }

    @Override
    public JournalEntry save(JournalEntry domain) {
        JournalEntryJpaEntity entity = toEntity(domain);
        JournalEntryJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<JournalEntry> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<JournalEntry> findByEntryNumber(String entryNumber) {
        return repository.findByEntryNumber(entryNumber).map(this::toDomain);
    }

    @Override
    public Optional<JournalEntry> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public List<JournalEntry> findByReference(String referenceType, String referenceId) {
        return repository.findByReferenceTypeAndReferenceId(referenceType, referenceId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public List<JournalEntry> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private JournalEntry toDomain(JournalEntryJpaEntity entity) {
        List<LedgerPosting> postings = entity.getPostings() != null
                ? entity.getPostings().stream().map(p -> new LedgerPosting(
                        p.getId(),
                        entity.getId(),
                        p.getAccount().getId(),
                        PostingDirection.valueOf(p.getDirection()),
                        p.getAmount(),
                        p.getCreatedAt()
                )).toList()
                : List.of();

        return new JournalEntry(
                entity.getId(),
                entity.getEntryNumber(),
                entity.getDescription(),
                entity.getReferenceType(),
                entity.getReferenceId(),
                entity.getIdempotencyKey(),
                entity.getTotalAmount(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                postings
        );
    }

    private JournalEntryJpaEntity toEntity(JournalEntry domain) {
        JournalEntryJpaEntity entity = JournalEntryJpaEntity.builder()
                .id(domain.getId())
                .entryNumber(domain.getEntryNumber())
                .description(domain.getDescription())
                .referenceType(domain.getReferenceType())
                .referenceId(domain.getReferenceId())
                .idempotencyKey(domain.getIdempotencyKey())
                .totalAmount(domain.getTotalAmount())
                .status(domain.getStatus())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .postings(new ArrayList<>())
                .build();

        for (LedgerPosting p : domain.getPostings()) {
            LedgerAccountJpaEntity accountRef = accountRepository.getReferenceById(p.getAccountId());
            LedgerPostingJpaEntity pe = LedgerPostingJpaEntity.builder()
                    .id(p.getId())
                    .journalEntry(entity)
                    .account(accountRef)
                    .direction(p.getDirection().name())
                    .amount(p.getAmount())
                    .createdAt(p.getCreatedAt())
                    .build();
            entity.getPostings().add(pe);
        }
        return entity;
    }
}

