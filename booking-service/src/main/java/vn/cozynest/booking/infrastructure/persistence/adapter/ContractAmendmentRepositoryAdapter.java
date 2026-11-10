package vn.cozynest.booking.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.booking.domain.model.ContractAmendment;
import vn.cozynest.booking.domain.repository.ContractAmendmentRepository;
import vn.cozynest.booking.infrastructure.persistence.mapper.ContractPersistenceMapper;
import vn.cozynest.booking.infrastructure.persistence.repository.SpringDataContractAmendmentRepository;

import java.util.List;
import java.util.Optional;

@Component
public class ContractAmendmentRepositoryAdapter implements ContractAmendmentRepository {

    private final SpringDataContractAmendmentRepository repository;

    public ContractAmendmentRepositoryAdapter(SpringDataContractAmendmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public ContractAmendment save(ContractAmendment amendment) {
        return ContractPersistenceMapper.toDomain(repository.save(ContractPersistenceMapper.toEntity(amendment)));
    }

    @Override
    public Optional<ContractAmendment> findById(String id) {
        return repository.findById(id).map(ContractPersistenceMapper::toDomain);
    }

    @Override
    public List<ContractAmendment> findByContractId(String contractId) {
        return repository.findByContractId(contractId).stream().map(ContractPersistenceMapper::toDomain).toList();
    }
}

