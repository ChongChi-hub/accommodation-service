package vn.cozynest.booking.application.dto;

import vn.cozynest.booking.domain.model.ContractOccupant;

import java.time.Instant;

public record ContractOccupantDto(
        String id,
        String contractId,
        String fullName,
        String phoneNumber,
        String idCardNumber,
        String relationship,
        boolean primaryTenant,
        Instant createdAt
) {
    public static ContractOccupantDto from(ContractOccupant o) {
        if (o == null) return null;
        return new ContractOccupantDto(
                o.getId(),
                o.getContractId(),
                o.getFullName(),
                o.getPhoneNumber(),
                o.getIdCardNumber(),
                o.getRelationship(),
                o.isPrimaryTenant(),
                o.getCreatedAt()
        );
    }
}

