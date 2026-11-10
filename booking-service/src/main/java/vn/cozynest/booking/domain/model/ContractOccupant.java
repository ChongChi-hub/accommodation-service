package vn.cozynest.booking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractOccupant {
    private String id;
    private String contractId;
    private String fullName;
    private String phoneNumber;
    private String idCardNumber;
    private String relationship;
    private boolean primaryTenant;
    private Instant createdAt;

    public static ContractOccupant create(String contractId, String fullName, String phoneNumber,
                                          String idCardNumber, String relationship, boolean primaryTenant) {
        return ContractOccupant.builder()
                .id(UUID.randomUUID().toString())
                .contractId(contractId)
                .fullName(fullName)
                .phoneNumber(phoneNumber)
                .idCardNumber(idCardNumber)
                .relationship(relationship)
                .primaryTenant(primaryTenant)
                .createdAt(Instant.now())
                .build();
    }
}

