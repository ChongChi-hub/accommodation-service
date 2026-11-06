package com.service.room.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WardEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "district_id", nullable = false, length = 36)
    private String districtId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", length = 10)
    private String code;
}

