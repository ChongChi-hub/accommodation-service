package vn.cozynest.booking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractAmendment {
    private String id;
    private String contractId;
    private String amendmentCode;
    private String amendmentType;
    private Long newRent;
    private LocalDate newEndDate;
    private LocalDate effectiveDate;
    private String reason;
    private String status;
    private Instant tenantSignedAt;
    private Instant landlordSignedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static ContractAmendment create(String contractId, String amendmentCode, String amendmentType,
                                           Long newRent, LocalDate newEndDate, LocalDate effectiveDate, String reason) {
        return ContractAmendment.builder()
                .id(UUID.randomUUID().toString())
                .contractId(contractId)
                .amendmentCode(amendmentCode)
                .amendmentType(amendmentType)
                .newRent(newRent)
                .newEndDate(newEndDate)
                .effectiveDate(effectiveDate)
                .reason(reason)
                .status("PENDING_SIGN")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public void signTenant() {
        this.tenantSignedAt = Instant.now();
        if (this.landlordSignedAt != null) {
            this.status = "ACTIVE";
        }
        this.updatedAt = Instant.now();
    }

    public void signLandlord() {
        this.landlordSignedAt = Instant.now();
        if (this.tenantSignedAt != null) {
            this.status = "ACTIVE";
        }
        this.updatedAt = Instant.now();
    }
}

