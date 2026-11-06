package com.service.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateAppointmentRequest {
    @NotBlank(message = "Thiếu roomId")
    private String roomId;

    @NotNull(message = "Thiếu ngày hẹn")
    private LocalDate appointmentDate;

    @NotNull(message = "Thiếu giờ hẹn")
    private LocalTime appointmentTime;

    private String message;
}

