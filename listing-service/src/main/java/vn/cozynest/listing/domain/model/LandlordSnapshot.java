package vn.cozynest.listing.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "landlord_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandlordSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "landlord_id", nullable = false, unique = true)
    private Long landlordId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 150)
    private String email;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean verified = true;

    @Column(name = "rating_avg")
    @Builder.Default
    private Double ratingAvg = 4.9;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 28;

    @Column(name = "response_rate_percent")
    @Builder.Default
    private Integer responseRatePercent = 99;

    @Column(name = "response_time_minutes")
    @Builder.Default
    private Integer responseTimeMinutes = 10;

    @Column(name = "room_count")
    @Builder.Default
    private Integer roomCount = 6;

    @Column(name = "joined_at")
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
