package vn.cozynest.booking.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.booking.domain.model.ContractOccupant;
import vn.cozynest.booking.domain.repository.ContractOccupantRepository;
import vn.cozynest.booking.infrastructure.persistence.mapper.ContractPersistenceMapper;
import vn.cozynest.booking.infrastructure.persistence.repository.SpringDataContractOccupantRepository;

import java.util.List;
import java.util.Optional;

@Component
public class ContractOccupantRepositoryAdapter implements ContractOccupantRepository {

    private final SpringDataContractOccupantRepository repository;

    public ContractOccupantRepositoryAdapter(SpringDataContractOccupantRepository repository) {
        this.repository = repository;
    }

    @Override
    public ContractOccupant save(ContractOccupant occupant) {
        return ContractPersistenceMapper.toDomain(repository.save(ContractPersistenceMapper.toEntity(occupant)));
    }

    @Override
    public Optional<ContractOccupant> findById(String id) {
        return repository.findById(id).map(ContractPersistenceMapper::toDomain);
    }

    @Override
    public List<ContractOccupant> findByContractId(String contractId) {
        return repository.findByContractId(contractId).stream().map(ContractPersistenceMapper::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}

