package vn.cozynest.listing.domain.repository;

import vn.cozynest.listing.domain.model.PropertyOwnershipDocument;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PropertyOwnershipDocumentRepository {
    List<PropertyOwnershipDocument> findByRoomId(Long roomId);
    List<PropertyOwnershipDocument> findByLandlordId(Long landlordId);
    List<PropertyOwnershipDocument> findByStatus(String status);
    Optional<PropertyOwnershipDocument> findById(String id);
    PropertyOwnershipDocument save(PropertyOwnershipDocument doc);
    List<PropertyOwnershipDocument> findByStatusAndExpiresAtBefore(String status, LocalDateTime before);
}
