package vn.cozynest.listing.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.cozynest.listing.infrastructure.persistence.entity.PropertyOwnershipDocumentJpaEntity;

import java.util.List;

public interface SpringDataPropertyOwnershipDocumentRepository
        extends JpaRepository<PropertyOwnershipDocumentJpaEntity, String> {
    List<PropertyOwnershipDocumentJpaEntity> findByRoomId(Long roomId);
    List<PropertyOwnershipDocumentJpaEntity> findByLandlordId(Long landlordId);
    List<PropertyOwnershipDocumentJpaEntity> findByStatus(String status);
}
