package vn.cozynest.booking.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.booking.domain.model.SagaInstance;
import vn.cozynest.booking.domain.repository.SagaInstanceRepository;
import vn.cozynest.booking.infrastructure.persistence.mapper.ContractPersistenceMapper;
import vn.cozynest.booking.infrastructure.persistence.repository.SpringDataSagaInstanceRepository;

import java.util.List;
import java.util.Optional;

@Component
public class SagaInstanceRepositoryAdapter implements SagaInstanceRepository {

    private final SpringDataSagaInstanceRepository repository;

    public SagaInstanceRepositoryAdapter(SpringDataSagaInstanceRepository repository) {
        this.repository = repository;
    }

    @Override
    public SagaInstance save(SagaInstance saga) {
        return ContractPersistenceMapper.toDomain(repository.save(ContractPersistenceMapper.toEntity(saga)));
    }

    @Override
    public Optional<SagaInstance> findBySagaId(String sagaId) {
        return repository.findBySagaId(sagaId).map(ContractPersistenceMapper::toDomain);
    }

    @Override
    public List<SagaInstance> findByStatus(String status) {
        return repository.findByStatus(status).stream().map(ContractPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<SagaInstance> findStuckInProgressBefore(java.time.Instant cutoff) {
        return repository.findStuckInProgressBefore(cutoff).stream().map(ContractPersistenceMapper::toDomain).toList();
    }
}

