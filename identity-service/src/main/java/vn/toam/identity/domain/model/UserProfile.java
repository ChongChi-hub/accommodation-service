package vn.toam.identity.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class UserProfile {

    private final String id;
    private final String userId;
    private String fullName;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String permanentAddress;
    private String addressDetail;
    private String provinceCode;
    private String wardId;
    private String occupation;
    private String bio;
    private String language;
    private String locale;
    private String timezone;
    private String avatarKey;
    private String avatarThumbKey;
    private String avatarMediumKey;
    private Long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public UserProfile(String id, String userId, String fullName, String phone, String gender,
                       LocalDate dateOfBirth, String permanentAddress, String addressDetail,
                       String provinceCode, String wardId, String occupation, String bio,
                       String language, String locale, String timezone, String avatarKey,
                       String avatarThumbKey, String avatarMediumKey, Long version,
                       Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.userId = userId;
        this.fullName = fullName;
        this.phone = phone;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.permanentAddress = permanentAddress;
        this.addressDetail = addressDetail;
        this.provinceCode = provinceCode;
        this.wardId = wardId;
        this.occupation = occupation;
        this.bio = bio;
        this.language = language != null ? language : "vi";
        this.locale = locale != null ? locale : "vi";
        this.timezone = timezone != null ? timezone : "Asia/Ho_Chi_Minh";
        this.avatarKey = avatarKey;
        this.avatarThumbKey = avatarThumbKey;
        this.avatarMediumKey = avatarMediumKey;
        this.version = version != null ? version : 0L;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static UserProfile createInitial(String userId, String fullName, String phone) {
        return new UserProfile(
                UUID.randomUUID().toString(),
                userId,
                fullName,
                phone,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "vi",
                "vi",
                "Asia/Ho_Chi_Minh",
                null,
                null,
                null,
                0L,
                Instant.now(),
                Instant.now()
        );
    }

    public void updateDetails(String fullName, String phone, String gender, LocalDate dateOfBirth,
                              String addressDetail, String provinceCode, String wardId,
                              String bio, String locale) {
        if (fullName != null && !fullName.isBlank()) {
            this.fullName = fullName.trim();
        }
        if (phone != null && !phone.isBlank()) {
            this.phone = phone.trim();
        }
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.addressDetail = addressDetail;
        this.provinceCode = provinceCode;
        this.wardId = wardId;
        this.bio = bio;
        if (locale != null && !locale.isBlank()) {
            this.locale = locale.trim();
            this.language = this.locale;
        }
        this.updatedAt = Instant.now();
    }

    public void setAvatarKeys(String avatarKey, String avatarThumbKey, String avatarMediumKey) {
        this.avatarKey = avatarKey;
        this.avatarThumbKey = avatarThumbKey;
        this.avatarMediumKey = avatarMediumKey;
        this.updatedAt = Instant.now();
    }

    public void removeAvatar() {
        this.avatarKey = null;
        this.avatarThumbKey = null;
        this.avatarMediumKey = null;
        this.updatedAt = Instant.now();
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getGender() { return gender; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getPermanentAddress() { return permanentAddress; }
    public String getAddressDetail() { return addressDetail; }
    public String getProvinceCode() { return provinceCode; }
    public String getWardId() { return wardId; }
    public String getOccupation() { return occupation; }
    public String getBio() { return bio; }
    public String getLanguage() { return language; }
    public String getLocale() { return locale; }
    public String getTimezone() { return timezone; }
    public String getAvatarKey() { return avatarKey; }
    public String getAvatarThumbKey() { return avatarThumbKey; }
    public String getAvatarMediumKey() { return avatarMediumKey; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setVersion(Long version) { this.version = version; }
}

