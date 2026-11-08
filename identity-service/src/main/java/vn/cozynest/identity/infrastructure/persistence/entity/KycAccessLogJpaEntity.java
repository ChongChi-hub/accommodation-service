package vn.cozynest.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.cozynest.identity.domain.model.KycAccessAction;

import java.time.Instant;

@Entity
@Table(name = "kyc_access_logs")
@Getter
@Setter
public class KycAccessLogJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "verification_id", length = 36, nullable = false)
    private String verificationId;

    @Column(name = "actor_id", length = 36, nullable = false)
    private String actorId;

    @Column(name = "actor_role", length = 50, nullable = false)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 20, nullable = false)
    private KycAccessAction action;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

