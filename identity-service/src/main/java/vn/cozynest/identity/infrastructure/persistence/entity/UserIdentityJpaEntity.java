package vn.cozynest.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_identities", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_identities_provider_sub", columnNames = {"provider", "provider_subject"}),
        @UniqueConstraint(name = "uq_user_identities_user_provider", columnNames = {"user_id", "provider"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentityJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_subject", nullable = false, length = 191)
    private String providerSubject;

    @Column(name = "email_at_link", length = 255)
    private String emailAtLink;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "picture_url", length = 1024)
    private String pictureUrl;

    @Column(length = 10)
    private String locale;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

