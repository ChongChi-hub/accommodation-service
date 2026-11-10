package vn.cozynest.billing.application.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class UtilityReadingDto {

    public record RecordReadingCommand(
            @NotNull(message = "Mã hợp đồng không được để trống")
            String contractId,

            @NotNull(message = "Mã phòng không được để trống")
            String roomId,

            @NotNull(message = "Kỳ ghi chỉ số không được để trống")
            LocalDate periodMonth,

            @NotNull(message = "Chỉ số điện cũ không được để trống")
            BigDecimal electricPrev,

            @NotNull(message = "Chỉ số điện mới không được để trống")
            BigDecimal electricCurr,

            @NotNull(message = "Chỉ số nước cũ không được để trống")
            BigDecimal waterPrev,

            @NotNull(message = "Chỉ số nước mới không được để trống")
            BigDecimal waterCurr,

            List<String> imageUrls
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
            Instant recordedAt
    ) {}
}

