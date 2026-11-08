package vn.cozynest.booking.application.dto;

public record AppointmentSummaryDto(
        long totalPending,
        long totalConfirmed,
        long totalNeedingOutcomeUpdate,
        long totalCompleted,
        long totalCancelled,
        long totalExpired,
        long todayCount
) {}
