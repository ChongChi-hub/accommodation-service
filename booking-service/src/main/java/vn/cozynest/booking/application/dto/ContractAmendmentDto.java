package vn.cozynest.booking.application.dto;

import vn.cozynest.booking.domain.model.ContractAmendment;

import java.time.Instant;
import java.time.LocalDate;

public record ContractAmendmentDto(
        String id,
        String contractId,
        String amendmentCode,
        String amendmentType,
        Long newRent,
        LocalDate newEndDate,
        LocalDate effectiveDate,
        String reason,
        String status,
        Instant tenantSignedAt,
        Instant landlordSignedAt,
        Instant createdAt
) {
    public static ContractAmendmentDto from(ContractAmendment a) {
        if (a == null) return null;
        return new ContractAmendmentDto(
                a.getId(),
                a.getContractId(),
                a.getAmendmentCode(),
                a.getAmendmentType(),
                a.getNewRent(),
                a.getNewEndDate(),
                a.getEffectiveDate(),
                a.getReason(),
                a.getStatus(),
                a.getTenantSignedAt(),
                a.getLandlordSignedAt(),
                a.getCreatedAt()
        );
    }
}

