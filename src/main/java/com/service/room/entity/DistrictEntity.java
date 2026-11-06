package com.service.room.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "districts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistrictEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "province_id", nullable = false, length = 36)
    private String provinceId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", length = 10)
    private String code;
}

