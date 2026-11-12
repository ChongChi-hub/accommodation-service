package vn.cozynest.listing.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyOwnershipDocument {

    private String id;
    private Long roomId;
    private Long landlordId;
    private String docType;
    private String fileUrl;
    private String maskedNumber;

    @Builder.Default
    private String status = "PENDING";

    private String rejectReason;
    private String reviewerId;
    private LocalDateTime reviewedAt;

    private LocalDateTime expiresAt;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
