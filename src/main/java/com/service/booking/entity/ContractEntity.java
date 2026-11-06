package com.service.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "room_id", nullable = false, length = 36)
    private String roomId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "landlord_id", nullable = false, length = 36)
    private String landlordId;

    @Column(name = "request_id", length = 36)
    private String requestId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 0)
    private BigDecimal monthlyRent;

    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal depositAmount;

    @Column(name = "terms", columnDefinition = "TEXT")
    private String terms;

    @Column(name = "status", nullable = false)
    private String status; // 'pending_sign', 'active', 'expired', 'terminated'

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

