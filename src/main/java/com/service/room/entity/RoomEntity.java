package com.service.room.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "landlord_id", nullable = false, length = 36)
    private String landlordId;

    @Column(name = "room_type_id", nullable = false, length = 36)
    private String roomTypeId;

    @Column(name = "ward_id", nullable = false, length = 36)
    private String wardId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", nullable = false, length = 255, unique = true)
    private String slug;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "area", nullable = false, precision = 6, scale = 2)
    private BigDecimal area;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "deposit", precision = 12, scale = 2)
    private BigDecimal deposit;

    @Column(name = "max_occupants", nullable = false)
    private Integer maxOccupants;

    @Column(name = "allow_pet", nullable = false)
    private Boolean allowPet;

    @Column(name = "allow_cooking", nullable = false)
    private Boolean allowCooking;

    @Column(name = "live_with_owner", nullable = false)
    private Boolean liveWithOwner;

    @Column(name = "curfew_time", length = 10)
    private String curfewTime;

    @Column(name = "extra_rules", columnDefinition = "TEXT")
    private String extraRules;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "status", nullable = false)
    private String status; // 'available', 'rented', 'maintenance', 'hidden'

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

