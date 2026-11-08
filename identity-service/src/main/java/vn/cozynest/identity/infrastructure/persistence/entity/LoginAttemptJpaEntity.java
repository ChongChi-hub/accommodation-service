package vn.cozynest.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "login_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttemptJpaEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String email;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "attempt_time", nullable = false)
    private Instant attemptTime;

    @Column(nullable = false)
    private boolean success;

    public static LoginAttemptJpaEntity create(String email, String ipAddress, boolean success) {
        return LoginAttemptJpaEntity.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .ipAddress(ipAddress != null ? ipAddress : "127.0.0.1")
                .attemptTime(Instant.now())
                .success(success)
                .build();
    }
}
