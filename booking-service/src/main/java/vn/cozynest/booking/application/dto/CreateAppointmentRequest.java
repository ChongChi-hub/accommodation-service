package vn.cozynest.booking.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateAppointmentRequest(
        @NotBlank(message = "Thieu roomId")
        String roomId,
        @NotBlank(message = "Thieu landlordId")
        String landlordId,
        @NotNull(message = "Thieu thoi gian bat dau hen")
        Instant scheduledStart,
        Instant scheduledEnd,
        @Size(max = 500)
        String tenantNote,
        @Size(max = 255)
        String roomTitle,
        @Size(max = 300)
        String roomAddress,
        @Size(max = 500)
        String roomCoverThumbKey,
        @Size(max = 100)
        String contactName,
        @Size(max = 20)
        String contactPhone,
        @Size(max = 100)
        String contactEmail
) {}

