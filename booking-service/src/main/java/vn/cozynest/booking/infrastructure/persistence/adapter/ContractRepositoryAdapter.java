package vn.cozynest.booking.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.booking.domain.model.Contract;
import vn.cozynest.booking.domain.repository.ContractRepository;
import vn.cozynest.booking.infrastructure.persistence.mapper.ContractPersistenceMapper;
import vn.cozynest.booking.infrastructure.persistence.repository.SpringDataContractRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class ContractRepositoryAdapter implements ContractRepository {

    private final SpringDataContractRepository repository;

    public ContractRepositoryAdapter(SpringDataContractRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Contract> findById(String id) {
        return repository.findById(id).map(ContractPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Contract> findByContractCode(String contractCode) {
        return repository.findByContractCode(contractCode).map(ContractPersistenceMapper::toDomain);
    }

    @Override
    public List<Contract> findByTenantId(String tenantId) {
        return repository.findByTenantId(tenantId).stream().map(ContractPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<Contract> findByLandlordId(String landlordId) {
        return repository.findByLandlordId(landlordId).stream().map(ContractPersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<Contract> findActiveByRoomId(String roomId) {
        return repository.findActiveByRoomId(roomId).map(ContractPersistenceMapper::toDomain);
    }

    @Override
    public List<Contract> findExpiringBetween(LocalDate start, LocalDate end) {
        return repository.findExpiringBetween(start, end).stream().map(ContractPersistenceMapper::toDomain).toList();
    }

    @Override
    public Contract save(Contract contract) {
        return ContractPersistenceMapper.toDomain(repository.save(ContractPersistenceMapper.toEntity(contract)));
    }
}

