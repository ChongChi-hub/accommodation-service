package vn.cozynest.booking.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateAmendmentDto(
        @NotBlank(message = "amendmentType khong duoc de trong")
        String amendmentType,

        Long newRent,

        LocalDate newEndDate,

        @NotNull(message = "effectiveDate khong duoc de trong")
        LocalDate effectiveDate,

        @NotBlank(message = "reason khong duoc de trong")
        String reason
) {}

