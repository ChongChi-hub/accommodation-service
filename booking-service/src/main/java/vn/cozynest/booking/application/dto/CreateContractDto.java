package vn.cozynest.booking.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateContractDto(
        @NotBlank(message = "roomId khong duoc de trong")
        String roomId,

        @NotBlank(message = "tenantId khong duoc de trong")
        String tenantId,

        @NotBlank(message = "landlordId khong duoc de trong")
        String landlordId,

        String requestId,

        @NotNull(message = "startDate khong duoc de trong")
        LocalDate startDate,

        @NotNull(message = "endDate khong duoc de trong")
        LocalDate endDate,

        @Min(value = 0, message = "monthlyRent phai >= 0")
        long monthlyRent,

        @Min(value = 0, message = "depositAmount phai >= 0")
        long depositAmount,

        String terms
) {}

