package vn.cozynest.booking.infrastructure.persistence.adapter;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import vn.cozynest.booking.domain.model.AppointmentStatus;
import vn.cozynest.booking.domain.model.ViewingAppointment;
import vn.cozynest.booking.domain.repository.ViewingAppointmentRepository;
import vn.cozynest.booking.infrastructure.persistence.entity.ViewingAppointmentJpaEntity;
import vn.cozynest.booking.infrastructure.persistence.mapper.AppointmentPersistenceMapper;
import vn.cozynest.booking.infrastructure.persistence.repository.SpringDataViewingAppointmentRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ViewingAppointmentRepositoryAdapter implements ViewingAppointmentRepository {

    private final SpringDataViewingAppointmentRepository springDataRepository;

    public ViewingAppointmentRepositoryAdapter(SpringDataViewingAppointmentRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<ViewingAppointment> findById(String id) {
        return springDataRepository.findById(id).map(AppointmentPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ViewingAppointment> findByIdAndLandlordId(String id, String landlordId) {
        return springDataRepository.findByIdAndLandlordId(id, landlordId)
                .map(AppointmentPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ViewingAppointment> findByIdAndTenantId(String id, String tenantId) {
        return springDataRepository.findByIdAndTenantId(id, tenantId)
                .map(AppointmentPersistenceMapper::toDomain);
    }

    @Override
    public ViewingAppointment save(ViewingAppointment appointment) {
        ViewingAppointmentJpaEntity entity = AppointmentPersistenceMapper.toEntity(appointment);
        ViewingAppointmentJpaEntity saved = springDataRepository.save(entity);
        return AppointmentPersistenceMapper.toDomain(saved);
    }

    @Override
    public List<ViewingAppointment> findOverlappingConfirmed(
            String landlordId, Instant start, Instant end, String excludeId) {
        return springDataRepository.findOverlappingConfirmed(landlordId, start, end, excludeId)
                .stream().map(AppointmentPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<ViewingAppointment> findOverlappingConfirmedForUpdate(
            String landlordId, Instant start, Instant end, String excludeId) {
        return springDataRepository.findOverlappingConfirmedForUpdate(landlordId, start, end, excludeId)
                .stream().map(AppointmentPersistenceMapper::toDomain).toList();
    }

    @Override
    public void lockOverlappingPending(String landlordId, Instant start, Instant end) {
        springDataRepository.findOverlappingPendingForUpdate(landlordId, start, end);
    }

    @Override
    public List<ViewingAppointment> findExpiredPending(Instant now, Instant pendingCutoff) {
        return springDataRepository.findExpiredPending(now, pendingCutoff)
                .stream().map(AppointmentPersistenceMapper::toDomain).toList();
    }

    @Override
    public int atomicallyExpirePending(String id, Instant now) {
        return springDataRepository.atomicallyExpirePending(id, now);
    }

    @Override
    public long countByLandlordIdAndStatus(String landlordId, AppointmentStatus status) {
        return springDataRepository.countByLandlordIdAndStatus(landlordId, status);
    }

    @Override
    public long countNeedingOutcomeUpdate(String landlordId, Instant now) {
        return springDataRepository.countNeedingOutcomeUpdate(landlordId, now);
    }

    @Override
    public long countTodayAppointments(String landlordId, Instant start, Instant end) {
        return springDataRepository.countTodayAppointments(landlordId, start, end);
    }

    @Override
    public List<ViewingAppointment> findByTenantId(String tenantId, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledStart"));
        return springDataRepository.findByTenantId(tenantId, pr)
                .map(AppointmentPersistenceMapper::toDomain).getContent();
    }

    @Override
    public long countByTenantId(String tenantId) {
        return springDataRepository.count((root, query, cb) -> cb.equal(root.get("tenantId"), tenantId));
    }

    @Override
    public List<ViewingAppointment> searchLandlordAppointments(
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
    ) {
        Specification<ViewingAppointmentJpaEntity> spec = buildLandlordSpec(landlordId, status, roomId, dateFrom, dateTo, search);
        String validSortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : "scheduledStart";
        Sort.Direction dir = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(dir, validSortBy));
        Page<ViewingAppointmentJpaEntity> result = springDataRepository.findAll(spec, pageRequest);
        return result.map(AppointmentPersistenceMapper::toDomain).getContent();
    }

    @Override
    public long countLandlordAppointments(
            String landlordId,
            AppointmentStatus status,
            String roomId,
            Instant dateFrom,
            Instant dateTo,
            String search
    ) {
        Specification<ViewingAppointmentJpaEntity> spec = buildLandlordSpec(landlordId, status, roomId, dateFrom, dateTo, search);
        return springDataRepository.count(spec);
    }

    private Specification<ViewingAppointmentJpaEntity> buildLandlordSpec(
            String landlordId,
            AppointmentStatus status,
            String roomId,
            Instant dateFrom,
            Instant dateTo,
            String search
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("landlordId"), landlordId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (roomId != null && !roomId.isBlank()) {
                predicates.add(cb.equal(root.get("roomId"), roomId.trim()));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("scheduledStart"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("scheduledStart"), dateTo));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("contactName")), pattern);
                Predicate phoneMatch = cb.like(cb.lower(root.get("contactPhone")), pattern);
                Predicate roomMatch = cb.like(cb.lower(root.get("roomTitle")), pattern);
                predicates.add(cb.or(nameMatch, phoneMatch, roomMatch));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

