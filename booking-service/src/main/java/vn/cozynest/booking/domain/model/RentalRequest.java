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
public class RentalRequest {
    private String id;
    private String roomId;
    private String tenantId;
    private String landlordId;
    private LocalDate proposedMoveInDate;
    private int numOccupants;
    private String message;
    private String status;
    private String contractId;
    private String idempotencyKey;
    private Instant holdingExpiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static RentalRequest create(String roomId, String tenantId, String landlordId,
                                       LocalDate proposedMoveInDate, int numOccupants,
                                       String message, String idempotencyKey) {
        return RentalRequest.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId)
                .tenantId(tenantId)
                .landlordId(landlordId)
                .proposedMoveInDate(proposedMoveInDate)
                .numOccupants(numOccupants > 0 ? numOccupants : 1)
                .message(message)
                .status("PENDING")
                .idempotencyKey(idempotencyKey)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public void approve(long holdingHours, String contractId) {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("Yêu cầu thuê phòng không ở trạng thái chờ duyệt");
        }
        this.status = "APPROVED";
        this.contractId = contractId;
        this.holdingExpiresAt = Instant.now().plusSeconds(holdingHours * 3600);
        this.updatedAt = Instant.now();
    }

    public void reject() {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("Yêu cầu thuê phòng không ở trạng thái chờ duyệt");
        }
        this.status = "REJECTED";
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = "CANCELLED";
        this.updatedAt = Instant.now();
    }

    public boolean isHoldingExpired() {
        return holdingExpiresAt != null && Instant.now().isAfter(holdingExpiresAt);
    }
}

