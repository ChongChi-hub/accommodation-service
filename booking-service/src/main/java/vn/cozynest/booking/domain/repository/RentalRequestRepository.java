package vn.cozynest.booking.domain.repository;

import vn.cozynest.booking.domain.model.RentalRequest;

import java.util.List;
import java.util.Optional;

public interface RentalRequestRepository {
    RentalRequest save(RentalRequest request);
    Optional<RentalRequest> findById(String id);
    Optional<RentalRequest> findByIdempotencyKey(String idempotencyKey);
    List<RentalRequest> findByTenantId(String tenantId);
    List<RentalRequest> findByLandlordId(String landlordId);
    List<RentalRequest> findByRoomId(String roomId);
}

