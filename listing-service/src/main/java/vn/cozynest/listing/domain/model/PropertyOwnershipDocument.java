package vn.cozynest.listing.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "property_ownership_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyOwnershipDocument {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "landlord_id", nullable = false)
    private Long landlordId;

    @Column(name = "doc_type", nullable = false, length = 50)
    private String docType;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "masked_number", length = 100)
    private String maskedNumber;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "reviewer_id", length = 36)
    private String reviewerId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}

