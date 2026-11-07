package vn.toam.booking.application.dto;

import vn.toam.booking.domain.model.AppointmentStatus;

import java.time.Instant;

public record AppointmentFilterParams(
        AppointmentStatus status,
        String roomId,
        Instant dateFrom,
        Instant dateTo,
        String search,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
) {}
