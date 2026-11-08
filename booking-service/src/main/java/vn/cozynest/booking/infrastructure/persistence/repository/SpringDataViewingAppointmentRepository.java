package vn.cozynest.booking.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import vn.cozynest.booking.domain.model.AppointmentStatus;
import vn.cozynest.booking.infrastructure.persistence.entity.ViewingAppointmentJpaEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataViewingAppointmentRepository
        extends JpaRepository<ViewingAppointmentJpaEntity, String>, JpaSpecificationExecutor<ViewingAppointmentJpaEntity> {

    Optional<ViewingAppointmentJpaEntity> findByIdAndLandlordId(String id, String landlordId);

    Optional<ViewingAppointmentJpaEntity> findByIdAndTenantId(String id, String tenantId);

    Page<ViewingAppointmentJpaEntity> findByTenantId(String tenantId, Pageable pageable);

    @Query("""
        SELECT a FROM ViewingAppointmentJpaEntity a
        WHERE a.landlordId = :landlordId
          AND a.status = vn.cozynest.booking.domain.model.AppointmentStatus.CONFIRMED
          AND (:excludeId IS NULL OR a.id <> :excludeId)
          AND a.scheduledStart < :end
          AND a.scheduledEnd > :start
    """)
    List<ViewingAppointmentJpaEntity> findOverlappingConfirmed(
            @Param("landlordId") String landlordId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("excludeId") String excludeId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a FROM ViewingAppointmentJpaEntity a
        WHERE a.landlordId = :landlordId
          AND a.status = vn.cozynest.booking.domain.model.AppointmentStatus.CONFIRMED
          AND (:excludeId IS NULL OR a.id <> :excludeId)
          AND a.scheduledStart < :end
          AND a.scheduledEnd > :start
    """)
    List<ViewingAppointmentJpaEntity> findOverlappingConfirmedForUpdate(
            @Param("landlordId") String landlordId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("excludeId") String excludeId
    );

    @Query("""
        SELECT a FROM ViewingAppointmentJpaEntity a
        WHERE a.status = vn.cozynest.booking.domain.model.AppointmentStatus.PENDING
          AND (a.scheduledStart <= :now OR a.createdAt <= :pendingCutoff)
    """)
    List<ViewingAppointmentJpaEntity> findExpiredPending(
            @Param("now") Instant now,
            @Param("pendingCutoff") Instant pendingCutoff
    );

    @Modifying
    @Query("""
        UPDATE ViewingAppointmentJpaEntity a
        SET a.status = vn.cozynest.booking.domain.model.AppointmentStatus.EXPIRED,
            a.cancelledBy = vn.cozynest.booking.domain.model.CancelledBy.SYSTEM,
            a.updatedAt = :now
        WHERE a.id = :id AND a.status = vn.cozynest.booking.domain.model.AppointmentStatus.PENDING
    """)
    int atomicallyExpirePending(@Param("id") String id, @Param("now") Instant now);

    long countByLandlordIdAndStatus(String landlordId, AppointmentStatus status);

    @Query("""
        SELECT COUNT(a) FROM ViewingAppointmentJpaEntity a
        WHERE a.landlordId = :landlordId
          AND a.status = vn.cozynest.booking.domain.model.AppointmentStatus.CONFIRMED
          AND a.scheduledStart <= :now
    """)
    long countNeedingOutcomeUpdate(@Param("landlordId") String landlordId, @Param("now") Instant now);

    @Query("""
        SELECT COUNT(a) FROM ViewingAppointmentJpaEntity a
        WHERE a.landlordId = :landlordId
          AND a.scheduledStart >= :start
          AND a.scheduledStart < :end
    """)
    long countTodayAppointments(
            @Param("landlordId") String landlordId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}
