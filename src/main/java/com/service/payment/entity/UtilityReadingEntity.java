package com.service.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "utility_readings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilityReadingEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "contract_id", nullable = false, length = 36)
    private String contractId;

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

    @Column(name = "reading_images", columnDefinition = "JSON")
    private String readingImages;

    @Column(name = "recorded_by", nullable = false, length = 36)
    private String recordedBy;

    @CreationTimestamp
    @Column(name = "recorded_at", updatable = false)
    private LocalDateTime recordedAt;
}

