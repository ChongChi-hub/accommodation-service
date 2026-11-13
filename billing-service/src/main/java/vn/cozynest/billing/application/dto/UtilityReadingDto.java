package vn.cozynest.billing.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vn.cozynest.billing.domain.service.UtilityTariffCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class UtilityReadingDto {

    public record RecordReadingCommand(
            @NotNull(message = "Mã hợp đồng không được để trống") String contractId,
            @NotNull(message = "Mã phòng không được để trống") String roomId,
            @NotNull(message = "Kỳ ghi chỉ số không được để trống") LocalDate periodMonth,
            @NotNull(message = "Chỉ số điện cũ không được để trống") BigDecimal electricPrev,
            @NotNull(message = "Chỉ số điện mới không được để trống") BigDecimal electricCurr,
            @NotNull(message = "Chỉ số nước cũ không được để trống") BigDecimal waterPrev,
            @NotNull(message = "Chỉ số nước mới không được để trống") BigDecimal waterCurr,
            List<String> imageUrls,
            UtilityTariffCalculator.PricingModel pricingModel,
            Long fixedElectricUnitPrice,
            Long fixedWaterUnitPrice,
            Long perPersonElectricRate,
            Long perPersonWaterRate,
            Integer occupantCount
    ) {
        public RecordReadingCommand(String contractId, String roomId, LocalDate periodMonth,
                                    BigDecimal electricPrev, BigDecimal electricCurr,
                                    BigDecimal waterPrev, BigDecimal waterCurr, List<String> imageUrls) {
            this(contractId, roomId, periodMonth, electricPrev, electricCurr, waterPrev, waterCurr,
                    imageUrls, UtilityTariffCalculator.PricingModel.EVN_TIERED, null, null, null, null, 1);
        }
    }

    public record EditReadingCommand(
            @NotNull BigDecimal electricPrev,
            @NotNull BigDecimal electricCurr,
            @NotNull BigDecimal waterPrev,
            @NotNull BigDecimal waterCurr,
            UtilityTariffCalculator.PricingModel pricingModel,
            Long fixedElectricUnitPrice,
            Long fixedWaterUnitPrice,
            Long perPersonElectricRate,
            Long perPersonWaterRate,
            Integer occupantCount,
            @NotBlank(message = "Ly do chinh sua chi so khong duoc de trong") String reason
    ) {}

    public record CancelReadingCommand(
            @NotBlank(message = "Ly do huy chi so khong duoc de trong") String reason
    ) {}

    public record DisputeReadingCommand(
            @NotBlank(message = "Ly do doi soat khong duoc de trong") String reason
    ) {}

    public record ReadingResponse(
            String id,
            String contractId,
            String roomId,
            LocalDate periodMonth,
            BigDecimal electricPrev,
            BigDecimal electricCurr,
            BigDecimal electricConsumption,
            BigDecimal waterPrev,
            BigDecimal waterCurr,
            BigDecimal waterConsumption,
            boolean hasSurgeAnomaly,
            Instant recordedAt,
            String status,
            String pricingModel,
            long electricityCost,
            long waterCost,
            int revision,
            List<String> imageUrls,
            Long fixedElectricUnitPrice,
            Long fixedWaterUnitPrice,
            String editReason
    ) {}
}
