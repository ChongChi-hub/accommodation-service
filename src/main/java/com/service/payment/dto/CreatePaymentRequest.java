package com.service.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePaymentRequest {
    @NotBlank(message = "invoiceId không được để trống")
    private String invoiceId;

    @NotNull(message = "amount không được để trống")
    private BigDecimal amount;

    @NotBlank(message = "method không được để trống")
    private String method; // cash, bank_transfer, momo, vnpay, zalopay
}

