package vn.cozynest.booking.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.booking.domain.model.AppointmentStatusHistory;
import vn.cozynest.booking.domain.repository.AppointmentStatusHistoryRepository;
import vn.cozynest.booking.infrastructure.persistence.entity.AppointmentStatusHistoryJpaEntity;
import vn.cozynest.booking.infrastructure.persistence.mapper.AppointmentPersistenceMapper;
import vn.cozynest.booking.infrastructure.persistence.repository.SpringDataAppointmentStatusHistoryRepository;

import java.util.List;

@Component
public class AppointmentStatusHistoryRepositoryAdapter implements AppointmentStatusHistoryRepository {

    private final SpringDataAppointmentStatusHistoryRepository repository;

    public AppointmentStatusHistoryRepositoryAdapter(SpringDataAppointmentStatusHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public AppointmentStatusHistory save(AppointmentStatusHistory history) {
        AppointmentStatusHistoryJpaEntity entity = AppointmentPersistenceMapper.toEntity(history);
        AppointmentStatusHistoryJpaEntity saved = repository.save(entity);
        return AppointmentPersistenceMapper.toDomain(saved);
    }

    @Override
    public List<AppointmentStatusHistory> findByAppointmentIdOrderByCreatedAtAsc(String appointmentId) {
        return repository.findByAppointmentIdOrderByCreatedAtAsc(appointmentId)
                .stream().map(AppointmentPersistenceMapper::toDomain).toList();
    }
}

