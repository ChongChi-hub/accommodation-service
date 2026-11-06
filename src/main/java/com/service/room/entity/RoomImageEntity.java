package com.service.room.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "room_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomImageEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "room_id", nullable = false, length = 36)
    private String roomId;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "is_cover", nullable = false)
    private Boolean isCover;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}

