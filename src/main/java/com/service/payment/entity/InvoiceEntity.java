package com.service.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "contract_id", nullable = false, length = 36)
    private String contractId;

    @Column(name = "utility_reading_id", length = 36)
    private String utilityReadingId;

    @Column(name = "period_month", nullable = false)
    private LocalDate periodMonth;

    @Column(name = "base_rent", nullable = false, precision = 12, scale = 0)
    private BigDecimal baseRent;

    @Column(name = "electric_usage", nullable = false, precision = 10, scale = 2)
    private BigDecimal electricUsage;

    @Column(name = "water_usage", nullable = false, precision = 10, scale = 2)
    private BigDecimal waterUsage;

    @Column(name = "electric_fee", nullable = false, precision = 12, scale = 0)
    private BigDecimal electricFee;

    @Column(name = "water_fee", nullable = false, precision = 12, scale = 0)
    private BigDecimal waterFee;

    @Column(name = "other_fees", nullable = false, precision = 12, scale = 0)
    private BigDecimal otherFees;

    @Column(name = "total", nullable = false, precision = 12, scale = 0)
    private BigDecimal total;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "status", nullable = false)
    private String status; // unpaid, paid, overdue, disputed

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

