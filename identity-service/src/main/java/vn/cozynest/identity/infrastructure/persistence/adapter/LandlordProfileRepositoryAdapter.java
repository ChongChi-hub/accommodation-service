package vn.cozynest.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.identity.domain.model.LandlordProfile;
import vn.cozynest.identity.domain.repository.LandlordProfileRepository;
import vn.cozynest.identity.infrastructure.persistence.entity.LandlordProfileJpaEntity;
import vn.cozynest.identity.infrastructure.persistence.repository.SpringDataLandlordProfileRepository;

import java.util.Optional;

@Component
public class LandlordProfileRepositoryAdapter implements LandlordProfileRepository {

    private final SpringDataLandlordProfileRepository repository;

    public LandlordProfileRepositoryAdapter(SpringDataLandlordProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<LandlordProfile> findById(String id) {
        return repository.findById(id).map(LandlordProfileJpaEntity::toDomain);
    }

    @Override
    public Optional<LandlordProfile> findByUserId(String userId) {
        return repository.findByUserId(userId).map(LandlordProfileJpaEntity::toDomain);
    }

    @Override
    public LandlordProfile save(LandlordProfile landlordProfile) {
        LandlordProfileJpaEntity entity = LandlordProfileJpaEntity.fromDomain(landlordProfile);
        LandlordProfileJpaEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public boolean existsByUserId(String userId) {
        return repository.existsByUserId(userId);
    }
}

