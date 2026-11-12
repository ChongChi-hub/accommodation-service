package vn.cozynest.booking.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.cozynest.booking.infrastructure.persistence.entity.RentalRequestJpaEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataRentalRequestRepository extends JpaRepository<RentalRequestJpaEntity, String> {
    Optional<RentalRequestJpaEntity> findByIdempotencyKey(String idempotencyKey);
    List<RentalRequestJpaEntity> findByTenantId(String tenantId);
    List<RentalRequestJpaEntity> findByLandlordId(String landlordId);
    List<RentalRequestJpaEntity> findByRoomId(String roomId);

    @Query("SELECT COUNT(r) FROM RentalRequestJpaEntity r WHERE r.tenantId = :tenantId AND r.createdAt >= :since")
    long countByTenantIdCreatedAfter(@Param("tenantId") String tenantId, @Param("since") Instant since);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END FROM RentalRequestJpaEntity r "
            + "WHERE r.roomId = :roomId AND r.tenantId = :tenantId AND r.status IN ('PENDING', 'APPROVED')")
    boolean existsActiveRequest(@Param("roomId") String roomId, @Param("tenantId") String tenantId);

    @Query("SELECT r FROM RentalRequestJpaEntity r WHERE r.status = 'APPROVED' AND r.holdingExpiresAt IS NOT NULL AND r.holdingExpiresAt < :now")
    List<RentalRequestJpaEntity> findExpiredApprovedHoldings(@Param("now") Instant now);
}

