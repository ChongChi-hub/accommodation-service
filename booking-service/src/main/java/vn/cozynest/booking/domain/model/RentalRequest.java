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
    private String roomTitle;
    private String tenantId;
    private String tenantName;
    private String landlordId;
    private LocalDate proposedMoveInDate;
    private int numOccupants;
    private String message;
    private String status;
    private String rejectionReason;
    private String contractId;
    private String idempotencyKey;
    private Instant holdingExpiresAt;
    private long version;
    private Instant createdAt;
    private Instant updatedAt;

    public static RentalRequest create(String roomId, String tenantId, String landlordId,
                                       LocalDate proposedMoveInDate, int numOccupants,
                                       String message, String idempotencyKey) {
        return create(roomId, null, tenantId, null, landlordId, proposedMoveInDate, numOccupants, message, idempotencyKey);
    }

    public static RentalRequest create(String roomId, String roomTitle, String tenantId, String tenantName,
                                       String landlordId, LocalDate proposedMoveInDate, int numOccupants,
                                       String message, String idempotencyKey) {
        return RentalRequest.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId)
                .roomTitle(roomTitle)
                .tenantId(tenantId)
                .tenantName(tenantName)
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
        reject(null);
    }

    public void reject(String reason) {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("Yêu cầu thuê phòng không ở trạng thái chờ duyệt");
        }
        this.status = "REJECTED";
        this.rejectionReason = (reason != null && !reason.isBlank()) ? reason.trim() : null;
        this.holdingExpiresAt = null;
        this.updatedAt = Instant.now();
    }

    public void linkContract(String contractId) {
        this.contractId = contractId;
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

