package com.service.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateUtilityReadingRequest {
    @NotBlank(message = "contractId không được để trống")
    private String contractId;

    @NotNull(message = "periodMonth không được để trống")
    private LocalDate periodMonth;

    private BigDecimal electricPrev = BigDecimal.ZERO;
    private BigDecimal electricCurr = BigDecimal.ZERO;
    private BigDecimal waterPrev = BigDecimal.ZERO;
    private BigDecimal waterCurr = BigDecimal.ZERO;

    private List<String> readingImages;
}

