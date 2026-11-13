package vn.cozynest.identity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.cozynest.identity.domain.model.LandlordProfile;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserRoleType;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;
    private String email;
    private String phone;
    private String fullName;
    private String avatarUrl;
    private String gender;
    private String dateOfBirth;
    private String status;
    private List<String> roles;
    private List<String> permissions;
    private boolean emailVerified;
    private boolean phoneMissing;
    private boolean hasPassword;
    private LandlordApplicationSummaryDto landlordApplication;
    private String workspaceHint;
    private String createdAt;

    public static UserDto fromDomain(User user) {
        return fromDomain(user, null);
    }

    public static UserDto fromDomain(User user, LandlordProfile landlordProfile) {
        List<String> roleNames = user.getRoles().stream().map(Enum::name).toList();
        List<String> permissions = new ArrayList<>();
        if (roleNames.contains("ADMIN")) {
            permissions.add("admin:all");
            permissions.add("kyc:review");
        } else if (roleNames.contains("MODERATOR")) {
            permissions.add("moderator:all");
            permissions.add("kyc:review");
        }
        if (roleNames.contains("LANDLORD")) {
            permissions.add("room:write");
            permissions.add("booking:manage");
        }
        if (roleNames.contains("TENANT")) {
            permissions.add("room:read");
            permissions.add("booking:create");
        }

        String workspaceHint = "TENANT";
        if (roleNames.contains("ADMIN")) {
            workspaceHint = "ADMIN";
        } else if (roleNames.contains("MODERATOR")) {
            workspaceHint = "MODERATOR";
        } else if (roleNames.contains("LANDLORD")) {
            workspaceHint = "LANDLORD";
        }

        boolean phoneMissing = user.getPhone() == null || user.getPhone().isBlank();
        boolean emailVerified = user.getEmailVerifiedAt() != null;

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null)
                .status(user.getStatus().name())
                .roles(roleNames)
                .permissions(permissions)
                .emailVerified(emailVerified)
                .phoneMissing(phoneMissing)
                .hasPassword(user.hasPassword())
                .landlordApplication(LandlordApplicationSummaryDto.fromDomain(landlordProfile))
                .workspaceHint(workspaceHint)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }
}
