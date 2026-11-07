package vn.toam.identity.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class User {
    private final String id;
    private String email;
    private String phone;
    private String passwordHash;
    private String fullName;
    private String avatarUrl;
    private String gender;
    private LocalDate dateOfBirth;
    private UserStatus status;
    private Instant emailVerifiedAt;
    private Instant lastLoginAt;
    private final Set<UserRoleType> roles;
    private final Instant createdAt;
    private Instant updatedAt;

    public User(String id, String email, String phone, String passwordHash, String fullName,
                String avatarUrl, String gender, LocalDate dateOfBirth, UserStatus status,
                Instant emailVerifiedAt, Instant lastLoginAt, Set<UserRoleType> roles,
                Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.emailVerifiedAt = emailVerifiedAt;
        this.lastLoginAt = lastLoginAt;
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static User createNew(String email, String phone, String passwordHash, String fullName, UserRoleType role) {
        Set<UserRoleType> roles = new HashSet<>();
        roles.add(role != null ? role : UserRoleType.TENANT);
        return new User(
                UUID.randomUUID().toString(),
                email,
                phone,
                passwordHash,
                fullName,
                null,
                null,
                null,
                UserStatus.ACTIVE,
                null,
                null,
                roles,
                Instant.now(),
                Instant.now()
        );
    }

    public void updateProfile(String fullName, String avatarUrl, String gender, LocalDate dateOfBirth) {
        this.fullName = fullName;
        if (avatarUrl != null) this.avatarUrl = avatarUrl;
        if (gender != null) this.gender = gender;
        if (dateOfBirth != null) this.dateOfBirth = dateOfBirth;
        this.updatedAt = Instant.now();
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = Instant.now();
    }

    public void recordLogin() {
        this.lastLoginAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addRole(UserRoleType role) {
        this.roles.add(role);
        this.updatedAt = Instant.now();
    }

    public void lock() {
        this.status = UserStatus.LOCKED;
        this.updatedAt = Instant.now();
    }

    public void unlock() {
        this.status = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    // Getters
    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getGender() { return gender; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public UserStatus getStatus() { return status; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Set<UserRoleType> getRoles() { return Set.copyOf(roles); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

