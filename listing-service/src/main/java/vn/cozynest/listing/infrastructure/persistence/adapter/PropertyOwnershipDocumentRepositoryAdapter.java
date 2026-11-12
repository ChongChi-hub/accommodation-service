package vn.cozynest.listing.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.PropertyOwnershipDocument;
import vn.cozynest.listing.domain.repository.PropertyOwnershipDocumentRepository;
import vn.cozynest.listing.infrastructure.persistence.mapper.ListingCatalogPersistenceMapper;
import vn.cozynest.listing.infrastructure.persistence.repository.SpringDataPropertyOwnershipDocumentRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PropertyOwnershipDocumentRepositoryAdapter implements PropertyOwnershipDocumentRepository {

    private final SpringDataPropertyOwnershipDocumentRepository springRepo;
    private final ListingCatalogPersistenceMapper mapper;

    @Override
    public List<PropertyOwnershipDocument> findByRoomId(Long roomId) {
        return springRepo.findByRoomId(roomId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<PropertyOwnershipDocument> findByLandlordId(Long landlordId) {
        return springRepo.findByLandlordId(landlordId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<PropertyOwnershipDocument> findByStatus(String status) {
        return springRepo.findByStatus(status).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<PropertyOwnershipDocument> findById(String id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public PropertyOwnershipDocument save(PropertyOwnershipDocument doc) {
        return mapper.toDomain(springRepo.save(mapper.toEntity(doc)));
    }
}
