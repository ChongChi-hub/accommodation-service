package vn.cozynest.booking.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.booking.domain.model.RentalRequest;
import vn.cozynest.booking.domain.repository.RentalRequestRepository;
import vn.cozynest.booking.infrastructure.persistence.mapper.ContractPersistenceMapper;
import vn.cozynest.booking.infrastructure.persistence.repository.SpringDataRentalRequestRepository;

import java.util.List;
import java.util.Optional;

@Component
public class RentalRequestRepositoryAdapter implements RentalRequestRepository {

    private final SpringDataRentalRequestRepository repository;

    public RentalRequestRepositoryAdapter(SpringDataRentalRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public RentalRequest save(RentalRequest request) {
        return ContractPersistenceMapper.toDomain(repository.save(ContractPersistenceMapper.toEntity(request)));
    }

    @Override
    public Optional<RentalRequest> findById(String id) {
        return repository.findById(id).map(ContractPersistenceMapper::toDomain);
    }

    @Override
    public Optional<RentalRequest> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(ContractPersistenceMapper::toDomain);
    }

    @Override
    public List<RentalRequest> findByTenantId(String tenantId) {
        return repository.findByTenantId(tenantId).stream().map(ContractPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<RentalRequest> findByLandlordId(String landlordId) {
        return repository.findByLandlordId(landlordId).stream().map(ContractPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<RentalRequest> findByRoomId(String roomId) {
        return repository.findByRoomId(roomId).stream().map(ContractPersistenceMapper::toDomain).toList();
    }

    @Override
    public long countByTenantIdCreatedAfter(String tenantId, java.time.Instant since) {
        return repository.countByTenantIdCreatedAfter(tenantId, since);
    }

    @Override
    public boolean existsActiveRequest(String roomId, String tenantId) {
        return repository.existsActiveRequest(roomId, tenantId);
    }

    @Override
    public List<RentalRequest> findExpiredApprovedHoldings(java.time.Instant now) {
        return repository.findExpiredApprovedHoldings(now).stream().map(ContractPersistenceMapper::toDomain).toList();
    }
}

