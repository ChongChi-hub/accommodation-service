package vn.toam.booking.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.toam.booking.domain.model.AppointmentStatus;
import vn.toam.booking.domain.model.ViewingAppointment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ViewingAppointmentRepository
        extends JpaRepository<ViewingAppointment, String>, JpaSpecificationExecutor<ViewingAppointment> {

    Optional<ViewingAppointment> findByIdAndLandlordId(String id, String landlordId);

    Optional<ViewingAppointment> findByIdAndTenantId(String id, String tenantId);

    Page<ViewingAppointment> findByTenantId(String tenantId, Pageable pageable);

    @Query("""
        SELECT a FROM ViewingAppointment a
        WHERE a.landlordId = :landlordId
          AND a.status = vn.toam.booking.domain.model.AppointmentStatus.CONFIRMED
          AND (:excludeId IS NULL OR a.id <> :excludeId)
          AND a.scheduledStart < :end
          AND a.scheduledEnd > :start
    """)
    List<ViewingAppointment> findOverlappingConfirmed(
            @Param("landlordId") String landlordId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("excludeId") String excludeId
    );

    @Query("""
        SELECT a FROM ViewingAppointment a
        WHERE a.status = vn.toam.booking.domain.model.AppointmentStatus.PENDING
          AND a.scheduledStart <= :now
    """)
    List<ViewingAppointment> findExpiredPending(@Param("now") Instant now);

    long countByLandlordIdAndStatus(String landlordId, AppointmentStatus status);

    @Query("""
        SELECT COUNT(a) FROM ViewingAppointment a
        WHERE a.landlordId = :landlordId
          AND a.status = vn.toam.booking.domain.model.AppointmentStatus.CONFIRMED
          AND a.scheduledStart <= :now
    """)
    long countNeedingOutcomeUpdate(@Param("landlordId") String landlordId, @Param("now") Instant now);

    @Query("""
        SELECT COUNT(a) FROM ViewingAppointment a
        WHERE a.landlordId = :landlordId
          AND a.scheduledStart >= :todayStart
          AND a.scheduledStart < :tomorrowStart
    """)
    long countTodayAppointments(
            @Param("landlordId") String landlordId,
            @Param("todayStart") Instant todayStart,
            @Param("tomorrowStart") Instant tomorrowStart
    );
}
