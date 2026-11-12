package vn.cozynest.booking.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.booking.domain.model.AppointmentEventOutbox;
import vn.cozynest.booking.domain.repository.AppointmentEventOutboxRepository;
import vn.cozynest.booking.infrastructure.persistence.entity.AppointmentEventOutboxJpaEntity;
import vn.cozynest.booking.infrastructure.persistence.mapper.AppointmentPersistenceMapper;
import vn.cozynest.booking.infrastructure.persistence.repository.SpringDataAppointmentEventOutboxRepository;

import java.util.List;
import java.util.Optional;

@Component
public class AppointmentEventOutboxRepositoryAdapter implements AppointmentEventOutboxRepository {

    private final SpringDataAppointmentEventOutboxRepository repository;

    public AppointmentEventOutboxRepositoryAdapter(SpringDataAppointmentEventOutboxRepository repository) {
        this.repository = repository;
    }

    @Override
    public AppointmentEventOutbox save(AppointmentEventOutbox outbox) {
        AppointmentEventOutboxJpaEntity entity = (outbox.getId() != null)
                ? repository.findByEventId(outbox.getId()).orElseGet(() -> AppointmentPersistenceMapper.toEntity(outbox))
                : AppointmentPersistenceMapper.toEntity(outbox);
        if (outbox.getProcessedAt() != null) {
            entity.setPublishedAt(outbox.getProcessedAt());
        }
        AppointmentEventOutboxJpaEntity saved = repository.save(entity);
        return AppointmentPersistenceMapper.toDomain(saved);
    }

    @Override
    public List<AppointmentEventOutbox> findUnprocessed() {
        return repository.findByPublishedAtIsNullOrderByCreatedAtAsc()
                .stream().map(AppointmentPersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<AppointmentEventOutbox> findById(String id) {
        return repository.findByEventId(id).map(AppointmentPersistenceMapper::toDomain);
    }
}
