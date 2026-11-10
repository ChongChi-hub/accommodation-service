package vn.cozynest.booking.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.cozynest.booking.infrastructure.persistence.entity.RentalRequestJpaEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataRentalRequestRepository extends JpaRepository<RentalRequestJpaEntity, String> {
    Optional<RentalRequestJpaEntity> findByIdempotencyKey(String idempotencyKey);
    List<RentalRequestJpaEntity> findByTenantId(String tenantId);
    List<RentalRequestJpaEntity> findByLandlordId(String landlordId);
    List<RentalRequestJpaEntity> findByRoomId(String roomId);
}

