package vn.cozynest.booking.application.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Phased PII disclosure DTO for a contract's check-in and room handover details.
 * Before contract status == ACTIVE: revealed=false and sensitive fields (fullAddress,
 * landlordPhone, checkinCode, checkinInstructions) are null.
 * After deposit PAID and contract ACTIVE: revealed=true with full handover details.
 */
public record ContractCheckinInfoDto(
        String contractId,
        String contractCode,
        String orderCode,
        String roomId,
        String status,
        boolean revealed,
        String areaHint,
        String fullAddress,
        String landlordName,
        String landlordPhone,
        String checkinCode,
        String checkinInstructions,
        LocalDate startDate,
        LocalDate endDate,
        long monthlyRent,
        long depositAmount,
        Instant depositPaidAt,
        String pdfUrl
) {}

