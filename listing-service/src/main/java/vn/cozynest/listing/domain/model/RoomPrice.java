package vn.cozynest.listing.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(name = "billing_mode", nullable = false, length = 30)
    @Builder.Default
    private String billingMode = "METERED";

    @Column(name = "is_metered", nullable = false)
    @Builder.Default
    private boolean metered = false;

    @Column(name = "meter_type", length = 20)
    private String meterType;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

