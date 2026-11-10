package vn.cozynest.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "utility_readings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilityReadingJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "contract_id", nullable = false, length = 36)
    private String contractId;

    @Column(name = "room_id", nullable = false, length = 64)
    private String roomId;

    @Column(name = "period_month", nullable = false)
    private LocalDate periodMonth;

    @Column(name = "electric_prev", nullable = false, precision = 10, scale = 2)
    private BigDecimal electricPrev;

    @Column(name = "electric_curr", nullable = false, precision = 10, scale = 2)
    private BigDecimal electricCurr;

    @Column(name = "water_prev", nullable = false, precision = 10, scale = 2)
    private BigDecimal waterPrev;

    @Column(name = "water_curr", nullable = false, precision = 10, scale = 2)
    private BigDecimal waterCurr;

    @Column(name = "reading_images_json", columnDefinition = "JSON")
    private String readingImagesJson;

    @Column(name = "recorded_by", nullable = false, length = 36)
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}

