package vn.cozynest.listing.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "tenant_name", nullable = false, length = 100)
    private String tenantName;

    @Column(name = "tenant_avatar", length = 500)
    private String tenantAvatar;

    @Column(nullable = false)
    private int rating;

    @Column(name = "is_verified_tenant", nullable = false)
    @Builder.Default
    private boolean verifiedTenant = true;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "landlord_reply", columnDefinition = "TEXT")
    private String landlordReply;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

