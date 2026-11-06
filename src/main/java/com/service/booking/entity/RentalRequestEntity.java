package com.service.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalRequestEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "room_id", nullable = false, length = 36)
    private String roomId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "move_in_date", nullable = false)
    private LocalDate moveInDate;

    @Column(name = "num_people", nullable = false)
    private Integer numPeople;

    @Column(name = "status", nullable = false)
    private String status; // 'pending', 'accepted', 'rejected', 'cancelled'

    @Column(name = "contract_id", length = 36)
    private String contractId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

