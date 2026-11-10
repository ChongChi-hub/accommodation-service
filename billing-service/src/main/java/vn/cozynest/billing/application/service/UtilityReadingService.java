package vn.cozynest.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.billing.application.dto.UtilityReadingDto;
import vn.cozynest.billing.domain.model.UtilityReading;
import vn.cozynest.billing.domain.repository.UtilityReadingRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class UtilityReadingService {
    private final UtilityReadingRepository repository;
    private final ObjectMapper objectMapper;

    public UtilityReadingService(UtilityReadingRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UtilityReadingDto.ReadingResponse recordReading(UtilityReadingDto.RecordReadingCommand command, String recordedBy) {
        String imagesJson = "[]";
        if (command.imageUrls() != null && !command.imageUrls().isEmpty()) {
            try {
                imagesJson = objectMapper.writeValueAsString(command.imageUrls());
            } catch (Exception ignored) {}
        }

        UtilityReading reading = new UtilityReading(
                UUID.randomUUID().toString(),
                command.contractId(),
                command.roomId(),
                command.periodMonth(),
                command.electricPrev(),
                command.electricCurr(),
                command.waterPrev(),
                command.waterCurr(),
                imagesJson,
                recordedBy,
                Instant.now()
        );

        // Surge anomaly detection against baseline averages (e.g. 100 kWh, 10 m3)
        boolean hasAnomaly = reading.isSurgeAnomaly(new BigDecimal("100"), new BigDecimal("10"));

        UtilityReading saved = repository.save(reading);

        return new UtilityReadingDto.ReadingResponse(
                saved.getId(), saved.getContractId(), saved.getRoomId(), saved.getPeriodMonth(),
                saved.getElectricPrev(), saved.getElectricCurr(), saved.getElectricConsumption(),
                saved.getWaterPrev(), saved.getWaterCurr(), saved.getWaterConsumption(),
                hasAnomaly, saved.getRecordedAt()
        );
    }

    @Transactional(readOnly = true)
    public UtilityReadingDto.ReadingResponse getLatestReading(String contractId) {
        return repository.findLatestByContractId(contractId)
                .map(r -> new UtilityReadingDto.ReadingResponse(
                        r.getId(), r.getContractId(), r.getRoomId(), r.getPeriodMonth(),
                        r.getElectricPrev(), r.getElectricCurr(), r.getElectricConsumption(),
                        r.getWaterPrev(), r.getWaterCurr(), r.getWaterConsumption(),
                        false, r.getRecordedAt()
                ))
                .orElse(null);
    }
}

