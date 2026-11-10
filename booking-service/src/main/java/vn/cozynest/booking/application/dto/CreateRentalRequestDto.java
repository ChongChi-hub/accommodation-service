package vn.cozynest.booking.application.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateRentalRequestDto(
        @NotBlank(message = "roomId khong duoc de trong")
        String roomId,

        @NotBlank(message = "landlordId khong duoc de trong")
        String landlordId,

        @NotNull(message = "proposedMoveInDate khong duoc de trong")
        @FutureOrPresent(message = "Ngay don vao phai tu hom nay tro di")
        LocalDate proposedMoveInDate,

        @Min(value = 1, message = "So nguoi o toi thieu la 1")
        int numOccupants,

        String message,

        String idempotencyKey
) {}

