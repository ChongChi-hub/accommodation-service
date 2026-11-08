package vn.cozynest.identity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AvatarUploadResponse {
    private String avatarKey;
    private String avatarUrl;
    private String avatarThumbUrl;
    private String avatarMediumUrl;
}

