package vn.cozynest.listing.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.listing.domain.model.PropertyOwnershipDocument;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PropertyOwnershipDocumentRepository extends JpaRepository<PropertyOwnershipDocument, String> {
    List<PropertyOwnershipDocument> findByRoomId(Long roomId);
    List<PropertyOwnershipDocument> findByLandlordId(Long landlordId);
    List<PropertyOwnershipDocument> findByStatus(String status);
    List<PropertyOwnershipDocument> findByStatusAndExpiresAtBefore(String status, LocalDateTime before);
}

