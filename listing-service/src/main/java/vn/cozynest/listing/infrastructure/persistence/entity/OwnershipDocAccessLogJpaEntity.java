package vn.cozynest.listing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ownership_doc_access_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnershipDocAccessLogJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "doc_id", length = 36)
    private String docId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "actor_id", nullable = false, length = 64)
    private String actorId;

    @Column(name = "actor_role", nullable = false, length = 32)
    private String actorRole;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "accessed_at", nullable = false)
    private LocalDateTime accessedAt;
}

