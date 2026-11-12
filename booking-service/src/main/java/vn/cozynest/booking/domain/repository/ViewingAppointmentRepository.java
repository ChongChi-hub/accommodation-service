package vn.cozynest.booking.domain.repository;

import vn.cozynest.booking.domain.model.AppointmentStatus;
import vn.cozynest.booking.domain.model.ViewingAppointment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ViewingAppointmentRepository {

    Optional<ViewingAppointment> findById(String id);

    Optional<ViewingAppointment> findByIdAndLandlordId(String id, String landlordId);

    Optional<ViewingAppointment> findByIdAndTenantId(String id, String tenantId);

    ViewingAppointment save(ViewingAppointment appointment);

    List<ViewingAppointment> findOverlappingConfirmed(String landlordId, Instant start, Instant end, String excludeId);

    List<ViewingAppointment> findOverlappingConfirmedForUpdate(String landlordId, Instant start, Instant end, String excludeId);

    default void lockOverlappingPending(String landlordId, Instant start, Instant end) {
    }

    List<ViewingAppointment> findExpiredPending(Instant now, Instant pendingCutoff);

    int atomicallyExpirePending(String id, Instant now);

    long countByLandlordIdAndStatus(String landlordId, AppointmentStatus status);

    long countNeedingOutcomeUpdate(String landlordId, Instant now);

    long countTodayAppointments(String landlordId, Instant start, Instant end);

    List<ViewingAppointment> findByTenantId(String tenantId, int page, int size);

    long countByTenantId(String tenantId);

    List<ViewingAppointment> searchLandlordAppointments(
            String landlordId,
            AppointmentStatus status,
            String roomId,
            Instant dateFrom,
            Instant dateTo,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    long countLandlordAppointments(
            String landlordId,
            AppointmentStatus status,
            String roomId,
            Instant dateFrom,
            Instant dateTo,
            String search
    );
}
