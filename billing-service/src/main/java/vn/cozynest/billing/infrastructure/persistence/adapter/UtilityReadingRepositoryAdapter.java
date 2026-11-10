package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.UtilityReading;
import vn.cozynest.billing.domain.repository.UtilityReadingRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.UtilityReadingJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataUtilityReadingRepository;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class UtilityReadingRepositoryAdapter implements UtilityReadingRepository {
    private final SpringDataUtilityReadingRepository repository;

    public UtilityReadingRepositoryAdapter(SpringDataUtilityReadingRepository repository) {
        this.repository = repository;
    }

    @Override
    public UtilityReading save(UtilityReading domain) {
        UtilityReadingJpaEntity entity = toEntity(domain);
        UtilityReadingJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<UtilityReading> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<UtilityReading> findByContractIdAndPeriodMonth(String contractId, LocalDate periodMonth) {
        return repository.findByContractIdAndPeriodMonth(contractId, periodMonth).map(this::toDomain);
    }

    @Override
    public Optional<UtilityReading> findLatestByContractId(String contractId) {
        return repository.findFirstByContractIdOrderByPeriodMonthDesc(contractId).map(this::toDomain);
    }

    private UtilityReading toDomain(UtilityReadingJpaEntity entity) {
        return new UtilityReading(
                entity.getId(),
                entity.getContractId(),
                entity.getRoomId(),
                entity.getPeriodMonth(),
                entity.getElectricPrev(),
                entity.getElectricCurr(),
                entity.getWaterPrev(),
                entity.getWaterCurr(),
                entity.getReadingImagesJson(),
                entity.getRecordedBy(),
                entity.getRecordedAt()
        );
    }

    private UtilityReadingJpaEntity toEntity(UtilityReading domain) {
        return UtilityReadingJpaEntity.builder()
                .id(domain.getId())
                .contractId(domain.getContractId())
                .roomId(domain.getRoomId())
                .periodMonth(domain.getPeriodMonth())
                .electricPrev(domain.getElectricPrev())
                .electricCurr(domain.getElectricCurr())
                .waterPrev(domain.getWaterPrev())
                .waterCurr(domain.getWaterCurr())
                .readingImagesJson(domain.getReadingImagesJson())
                .recordedBy(domain.getRecordedBy())
                .recordedAt(domain.getRecordedAt())
                .build();
    }
}

