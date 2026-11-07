package vn.toam.identity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.toam.identity.domain.model.User;

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
    private String createdAt;

    public static UserDto fromDomain(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null)
                .status(user.getStatus().name())
                .roles(user.getRoles().stream().map(Enum::name).toList())
                .createdAt(user.getCreatedAt().toString())
                .build();
    }
}

