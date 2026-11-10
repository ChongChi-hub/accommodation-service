package vn.cozynest.billing.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class UtilityReading {
    private final String id;
    private final String contractId;
    private final String roomId;
    private final LocalDate periodMonth;
    private final BigDecimal electricPrev;
    private final BigDecimal electricCurr;
    private final BigDecimal waterPrev;
    private final BigDecimal waterCurr;
    private final String readingImagesJson;
    private final String recordedBy;
    private final Instant recordedAt;

    public UtilityReading(String id, String contractId, String roomId, LocalDate periodMonth,
                          BigDecimal electricPrev, BigDecimal electricCurr,
                          BigDecimal waterPrev, BigDecimal waterCurr,
                          String readingImagesJson, String recordedBy, Instant recordedAt) {
        if (electricCurr.compareTo(electricPrev) < 0) {
            throw new IllegalArgumentException("Chi so dien moi khong the nho hon chi so cu");
        }
        if (waterCurr.compareTo(waterPrev) < 0) {
            throw new IllegalArgumentException("Chi so nuoc moi khong the nho hon chi so cu");
        }
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.contractId = contractId;
        this.roomId = roomId;
        this.periodMonth = periodMonth;
        this.electricPrev = electricPrev;
        this.electricCurr = electricCurr;
        this.waterPrev = waterPrev;
        this.waterCurr = waterCurr;
        this.readingImagesJson = readingImagesJson;
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt != null ? recordedAt : Instant.now();
    }

    public BigDecimal getElectricConsumption() {
        return electricCurr.subtract(electricPrev);
    }

    public BigDecimal getWaterConsumption() {
        return waterCurr.subtract(waterPrev);
    }

    public boolean isSurgeAnomaly(BigDecimal avgElectric, BigDecimal avgWater) {
        if (avgElectric != null && avgElectric.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal electricRatio = getElectricConsumption().divide(avgElectric, 2, java.math.RoundingMode.HALF_UP);
            if (electricRatio.compareTo(new BigDecimal("2.0")) > 0) return true;
        }
        if (avgWater != null && avgWater.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal waterRatio = getWaterConsumption().divide(avgWater, 2, java.math.RoundingMode.HALF_UP);
            if (waterRatio.compareTo(new BigDecimal("2.0")) > 0) return true;
        }
        return false;
    }

    public String getId() { return id; }
    public String getContractId() { return contractId; }
    public String getRoomId() { return roomId; }
    public LocalDate getPeriodMonth() { return periodMonth; }
    public BigDecimal getElectricPrev() { return electricPrev; }
    public BigDecimal getElectricCurr() { return electricCurr; }
    public BigDecimal getWaterPrev() { return waterPrev; }
    public BigDecimal getWaterCurr() { return waterCurr; }
    public String getReadingImagesJson() { return readingImagesJson; }
    public String getRecordedBy() { return recordedBy; }
    public Instant getRecordedAt() { return recordedAt; }
}

