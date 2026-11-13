package vn.cozynest.identity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.cozynest.identity.domain.model.UserIdentity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentityDto {
    private String id;
    private String provider;
    private String providerSubject;
    private String emailAtLink;
    private boolean emailVerified;
    private String displayName;
    private String pictureUrl;
    private String locale;
    private String linkedAt;
    private String lastLoginAt;

    public static UserIdentityDto fromDomain(UserIdentity domain) {
        if (domain == null) return null;
        return UserIdentityDto.builder()
                .id(domain.getId())
                .provider(domain.getProvider())
                .providerSubject(domain.getProviderSubject())
                .emailAtLink(domain.getEmailAtLink())
                .emailVerified(domain.isEmailVerified())
                .displayName(domain.getDisplayName())
                .pictureUrl(domain.getPictureUrl())
                .locale(domain.getLocale())
                .linkedAt(domain.getLinkedAt() != null ? domain.getLinkedAt().toString() : null)
                .lastLoginAt(domain.getLastLoginAt() != null ? domain.getLastLoginAt().toString() : null)
                .build();
    }
}

