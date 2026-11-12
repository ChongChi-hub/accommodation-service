package vn.cozynest.billing.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.cozynest.billing.domain.model.UtilityReading;
import vn.cozynest.billing.domain.repository.UtilityReadingRepository;
import vn.cozynest.billing.infrastructure.persistence.entity.UtilityReadingJpaEntity;
import vn.cozynest.billing.infrastructure.persistence.repository.SpringDataUtilityReadingRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class UtilityReadingRepositoryAdapter implements UtilityReadingRepository {
    private final SpringDataUtilityReadingRepository repository;

    public UtilityReadingRepositoryAdapter(SpringDataUtilityReadingRepository repository) {
        this.repository = repository;
    }

    @Override
    public UtilityReading save(UtilityReading domain) {
        return toDomain(repository.save(toEntity(domain)));
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

    @Override
    public List<UtilityReading> findByContractId(String contractId) {
        return repository.findByContractIdOrderByPeriodMonthDesc(contractId).stream().map(this::toDomain).toList();
    }

    private UtilityReading toDomain(UtilityReadingJpaEntity e) {
        return new UtilityReading(
                e.getId(), e.getContractId(), e.getRoomId(), e.getPeriodMonth(),
                e.getElectricPrev(), e.getElectricCurr(), e.getWaterPrev(), e.getWaterCurr(),
                e.getReadingImagesJson(), e.getRecordedBy(), e.getRecordedAt(),
                e.getStatus(), e.getPricingModel(), e.getTariffConfigJson(),
                e.getElectricityCost(), e.getWaterCost(), e.getRevision(), e.getEditReason()
        );
    }

    private UtilityReadingJpaEntity toEntity(UtilityReading d) {
        return UtilityReadingJpaEntity.builder()
                .id(d.getId()).contractId(d.getContractId()).roomId(d.getRoomId())
                .periodMonth(d.getPeriodMonth()).electricPrev(d.getElectricPrev()).electricCurr(d.getElectricCurr())
                .waterPrev(d.getWaterPrev()).waterCurr(d.getWaterCurr())
                .readingImagesJson(d.getReadingImagesJson()).recordedBy(d.getRecordedBy())
                .recordedAt(d.getRecordedAt()).status(d.getStatus()).pricingModel(d.getPricingModel())
                .tariffConfigJson(d.getTariffConfigJson()).electricityCost(d.getElectricityCost())
                .waterCost(d.getWaterCost()).revision(d.getRevision()).editReason(d.getEditReason())
                .build();
    }
}
