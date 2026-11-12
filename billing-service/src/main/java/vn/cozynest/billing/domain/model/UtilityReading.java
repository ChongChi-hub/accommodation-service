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
    private BigDecimal electricPrev;
    private BigDecimal electricCurr;
    private BigDecimal waterPrev;
    private BigDecimal waterCurr;
    private String readingImagesJson;
    private final String recordedBy;
    private final Instant recordedAt;
    private String status;
    private String pricingModel;
    private String tariffConfigJson;
    private long electricityCost;
    private long waterCost;
    private int revision;
    private String editReason;

    public UtilityReading(String id, String contractId, String roomId, LocalDate periodMonth,
                          BigDecimal electricPrev, BigDecimal electricCurr,
                          BigDecimal waterPrev, BigDecimal waterCurr,
                          String readingImagesJson, String recordedBy, Instant recordedAt) {
        this(id, contractId, roomId, periodMonth, electricPrev, electricCurr, waterPrev, waterCurr,
                readingImagesJson, recordedBy, recordedAt, "ACTIVE", "EVN_TIERED", null, 0L, 0L, 1, null);
    }

    public UtilityReading(String id, String contractId, String roomId, LocalDate periodMonth,
                          BigDecimal electricPrev, BigDecimal electricCurr,
                          BigDecimal waterPrev, BigDecimal waterCurr,
                          String readingImagesJson, String recordedBy, Instant recordedAt,
                          String status, String pricingModel, String tariffConfigJson,
                          long electricityCost, long waterCost, int revision, String editReason) {
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
        this.status = status != null ? status : "ACTIVE";
        this.pricingModel = pricingModel != null ? pricingModel : "EVN_TIERED";
        this.tariffConfigJson = tariffConfigJson;
        this.electricityCost = electricityCost;
        this.waterCost = waterCost;
        this.revision = revision > 0 ? revision : 1;
        this.editReason = editReason;
    }

    public void edit(BigDecimal newElectricPrev, BigDecimal newElectricCurr,
                     BigDecimal newWaterPrev, BigDecimal newWaterCurr,
                     String newPricingModel, String newTariffConfigJson,
                     long newElectricityCost, long newWaterCost, String reason) {
        if ("CANCELLED".equals(this.status)) {
            throw new IllegalStateException("Khong the sua chi so da bi huy");
        }
        if (newElectricCurr.compareTo(newElectricPrev) < 0 || newWaterCurr.compareTo(newWaterPrev) < 0) {
            throw new IllegalArgumentException("Chi so moi khong the nho hon chi so cu");
        }
        this.electricPrev = newElectricPrev;
        this.electricCurr = newElectricCurr;
        this.waterPrev = newWaterPrev;
        this.waterCurr = newWaterCurr;
        if (newPricingModel != null) this.pricingModel = newPricingModel;
        this.tariffConfigJson = newTariffConfigJson;
        this.electricityCost = newElectricityCost;
        this.waterCost = newWaterCost;
        this.revision++;
        this.editReason = reason;
    }

    public void cancel(String reason) {
        if ("CANCELLED".equals(this.status)) {
            return;
        }
        this.status = "CANCELLED";
        this.editReason = reason;
        this.revision++;
    }

    public BigDecimal getElectricConsumption() { return electricCurr.subtract(electricPrev); }
    public BigDecimal getWaterConsumption() { return waterCurr.subtract(waterPrev); }

    public boolean isSurgeAnomaly(BigDecimal avgElectric, BigDecimal avgWater) {
        if (avgElectric != null && avgElectric.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = getElectricConsumption().divide(avgElectric, 2, java.math.RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("2.0")) > 0) return true;
        }
        if (avgWater != null && avgWater.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = getWaterConsumption().divide(avgWater, 2, java.math.RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("2.0")) > 0) return true;
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
    public String getStatus() { return status; }
    public String getPricingModel() { return pricingModel; }
    public String getTariffConfigJson() { return tariffConfigJson; }
    public long getElectricityCost() { return electricityCost; }
    public long getWaterCost() { return waterCost; }
    public int getRevision() { return revision; }
    public String getEditReason() { return editReason; }
}
