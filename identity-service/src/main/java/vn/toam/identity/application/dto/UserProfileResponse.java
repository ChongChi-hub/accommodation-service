package vn.toam.identity.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Builder
public class UserProfileResponse {
    private String id;
    private String userId;
    private String email;
    private String fullName;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String addressDetail;
    private String provinceCode;
    private String wardId;
    private String permanentAddress;
    private String occupation;
    private String bio;
    private String locale;
    private String avatarKey;
    private String avatarUrl;
    private String avatarThumbUrl;
    private String avatarMediumUrl;
    private String kycStatus;
    private Set<String> roles;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}

