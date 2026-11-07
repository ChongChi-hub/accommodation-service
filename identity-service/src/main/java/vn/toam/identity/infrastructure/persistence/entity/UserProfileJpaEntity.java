package vn.toam.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
public class UserProfileJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false, unique = true)
    private String userId;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "phone", length = 20, unique = true)
    private String phone;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "permanent_address", length = 255)
    private String permanentAddress;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "province_code", length = 20)
    private String provinceCode;

    @Column(name = "ward_id", length = 50)
    private String wardId;

    @Column(name = "occupation", length = 100)
    private String occupation;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "language", length = 10, nullable = false)
    private String language = "vi";

    @Column(name = "locale", length = 10, nullable = false)
    private String locale = "vi";

    @Column(name = "timezone", length = 50, nullable = false)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "avatar_key", length = 255)
    private String avatarKey;

    @Column(name = "avatar_thumb_key", length = 255)
    private String avatarThumbKey;

    @Column(name = "avatar_medium_key", length = 255)
    private String avatarMediumKey;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
