package com.service.room.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "room_prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomPriceEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "room_id", nullable = false, length = 36)
    private String roomId;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "price", nullable = false, precision = 12, scale = 0)
    private BigDecimal price;

    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    @Column(name = "is_metered", nullable = false)
    private Boolean isMetered;

    @Column(name = "meter_type", length = 20)
    private String meterType;
}

