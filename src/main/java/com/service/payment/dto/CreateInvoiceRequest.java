package com.service.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateInvoiceRequest {
    @NotBlank(message = "contractId không được để trống")
    private String contractId;

    @NotNull(message = "periodMonth không được để trống")
    private LocalDate periodMonth;

    @NotNull(message = "baseRent không được để trống")
    private BigDecimal baseRent;

    private BigDecimal electricUsage = BigDecimal.ZERO;
    private BigDecimal waterUsage = BigDecimal.ZERO;
    private BigDecimal electricFee = BigDecimal.ZERO;
    private BigDecimal waterFee = BigDecimal.ZERO;
    private BigDecimal otherFees = BigDecimal.ZERO;

    @NotNull(message = "dueDate không được để trống")
    private LocalDate dueDate;

    private String utilityReadingId;
}

