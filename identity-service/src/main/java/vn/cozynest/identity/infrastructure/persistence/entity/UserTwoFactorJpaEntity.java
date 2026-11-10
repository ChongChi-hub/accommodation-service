package vn.cozynest.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_two_factors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTwoFactorJpaEntity {
    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "totp_secret", nullable = false, length = 128)
    private String totpSecret;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Column(name = "backup_codes_json", columnDefinition = "JSON")
    private String backupCodesJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

