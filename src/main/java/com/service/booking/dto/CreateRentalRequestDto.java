package com.service.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateRentalRequestDto {
    @NotBlank(message = "Thiếu roomId")
    private String roomId;

    @NotNull(message = "Thiếu ngày chuyển vào")
    private LocalDate moveInDate;

    private Integer numPeople = 1;
    private String message;
}

