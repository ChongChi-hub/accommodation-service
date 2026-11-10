package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.AccountType;
import vn.cozynest.billing.domain.model.LedgerAccount;
import vn.cozynest.billing.domain.repository.LedgerAccountRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.LedgerAccountJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataLedgerAccountRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class LedgerAccountRepositoryAdapter implements LedgerAccountRepository {
    private final SpringDataLedgerAccountRepository repository;

    public LedgerAccountRepositoryAdapter(SpringDataLedgerAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public LedgerAccount save(LedgerAccount domain) {
        LedgerAccountJpaEntity entity = toEntity(domain);
        LedgerAccountJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<LedgerAccount> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<LedgerAccount> findByAccountNumber(String accountNumber) {
        return repository.findByAccountNumber(accountNumber).map(this::toDomain);
    }

    @Override
    public Optional<LedgerAccount> findByOwnerIdAndType(String ownerId, AccountType type) {
        return repository.findByOwnerIdAndAccountType(ownerId, type.name()).map(this::toDomain);
    }

    @Override
    public List<LedgerAccount> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private LedgerAccount toDomain(LedgerAccountJpaEntity entity) {
        return new LedgerAccount(
                entity.getId(),
                entity.getAccountNumber(),
                entity.getAccountName(),
                AccountType.valueOf(entity.getAccountType()),
                entity.getOwnerId(),
                entity.getBalance(),
                entity.getCurrency(),
                entity.isFrozen(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private LedgerAccountJpaEntity toEntity(LedgerAccount domain) {
        return LedgerAccountJpaEntity.builder()
                .id(domain.getId())
                .accountNumber(domain.getAccountNumber())
                .accountName(domain.getAccountName())
                .accountType(domain.getAccountType().name())
                .ownerId(domain.getOwnerId())
                .balance(domain.getBalance())
                .currency(domain.getCurrency())
                .frozen(domain.isFrozen())
                .version(domain.getVersion())
                .createdAt(domain.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
    }
}

