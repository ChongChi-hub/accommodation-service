package vn.cozynest.billing.application.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

public class InvoiceDto {

    public record GenerateInvoiceCommand(
            @NotNull(message = "Mã hợp đồng không được để trống")
            String contractId,

            String utilityReadingId,

            @NotNull(message = "Người thuê không được để trống")
            String tenantId,

            @NotNull(message = "Chủ trọ không được để trống")
            String landlordId,

            @NotNull(message = "Kỳ hóa đơn không được để trống")
            LocalDate periodMonth,

            long baseRent,
            long electricityAmount,
            long waterAmount,
            long serviceAmount,
            long otherAmount,

            @NotNull(message = "Hạn thanh toán không được để trống")
            LocalDate dueDate
    ) {}

    public record InvoiceResponse(
            String id,
            String invoiceCode,
            String contractId,
            String utilityReadingId,
            String tenantId,
            String landlordId,
            LocalDate periodMonth,
            long baseRent,
            long electricityAmount,
            long waterAmount,
            long serviceAmount,
            long otherAmount,
            long totalAmount,
            long paidAmount,
            LocalDate dueDate,
            String status,
            Instant createdAt
    ) {}
}

