package vn.cozynest.booking.application.dto;

import vn.cozynest.booking.domain.model.RentalRequest;

import java.time.Instant;
import java.time.LocalDate;

public record RentalRequestDto(
        String id,
        String roomId,
        String roomTitle,
        String tenantId,
        String tenantName,
        String landlordId,
        LocalDate proposedMoveInDate,
        int numOccupants,
        String message,
        String status,
        String rejectionReason,
        String contractId,
        Instant holdingExpiresAt,
        Instant createdAt
) {
    public static RentalRequestDto from(RentalRequest r) {
        if (r == null) return null;
        return new RentalRequestDto(
                r.getId(),
                r.getRoomId(),
                r.getRoomTitle(),
                r.getTenantId(),
                r.getTenantName(),
                r.getLandlordId(),
                r.getProposedMoveInDate(),
                r.getNumOccupants(),
                r.getMessage(),
                r.getStatus(),
                r.getRejectionReason(),
                r.getContractId(),
                r.getHoldingExpiresAt(),
                r.getCreatedAt()
        );
    }
}
