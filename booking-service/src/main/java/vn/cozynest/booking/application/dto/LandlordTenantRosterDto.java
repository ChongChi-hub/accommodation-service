package vn.cozynest.booking.application.dto;

import java.time.Instant;
import java.time.LocalDate;

public record LandlordTenantRosterDto(
        String contractId,
        String contractCode,
        String orderCode,
        String roomId,
        String roomTitle,
        String tenantId,
        String tenantName,
        String tenantEmail,
        String tenantPhone,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        long monthlyRent,
        long depositAmount,
        Instant depositPaidAt
) {}
