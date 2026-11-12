package vn.cozynest.billing.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class InvoiceDto {

    public record GenerateInvoiceCommand(
            @NotNull(message = "Mã hợp đồng không được để trống") String contractId,
            String utilityReadingId,
            @NotNull(message = "Người thuê không được để trống") String tenantId,
            @NotNull(message = "Chủ trọ không được để trống") String landlordId,
            @NotNull(message = "Kỳ hóa đơn không được để trống") LocalDate periodMonth,
            long baseRent,
            long electricityAmount,
            long waterAmount,
            long serviceAmount,
            long otherAmount,
            @NotNull(message = "Hạn thanh toán không được để trống") LocalDate dueDate
    ) {}

    public record BatchGenerateCommand(
            @NotEmpty @Valid List<GenerateInvoiceCommand> items
    ) {}

    public record CreditNoteCommand(
            @Min(value = 1, message = "So tien dieu chinh giam phai lon hon 0") long amount,
            @NotBlank(message = "Ly do lap phieu dieu chinh giam khong duoc de trong") String reason
    ) {}

    public record RecordPaymentCommand(
            @Min(value = 1, message = "So tien thanh toan phai lon hon 0") long amount
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
            String invoiceType,
            String referenceInvoiceId,
            String creditNoteReason,
            Instant createdAt
    ) {}
}
