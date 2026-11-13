package vn.cozynest.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class UserIdentity {
    private final String id;
    private final String userId;
    private final String provider;
    private final String providerSubject;
    private String emailAtLink;
    private boolean emailVerified;
    private String displayName;
    private String pictureUrl;
    private String locale;
    private final Instant linkedAt;
    private Instant lastLoginAt;
    private final Instant createdAt;

    public UserIdentity(String id, String userId, String provider, String providerSubject,
                        String emailAtLink, boolean emailVerified, String displayName,
                        String pictureUrl, String locale, Instant linkedAt,
                        Instant lastLoginAt, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.userId = userId;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.emailAtLink = emailAtLink;
        this.emailVerified = emailVerified;
        this.displayName = displayName;
        this.pictureUrl = pictureUrl;
        this.locale = locale;
        this.linkedAt = linkedAt != null ? linkedAt : Instant.now();
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static UserIdentity createGoogleIdentity(String userId, String sub, String email,
                                                    String name, String pictureUrl, String locale) {
        return new UserIdentity(
                UUID.randomUUID().toString(),
                userId,
                "GOOGLE",
                sub,
                email,
                true,
                name,
                pictureUrl,
                locale,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
    }

    public void updateLogin(String displayName, String pictureUrl, String locale) {
        this.lastLoginAt = Instant.now();
        if (displayName != null && !displayName.isBlank()) this.displayName = displayName;
        if (pictureUrl != null && !pictureUrl.isBlank()) this.pictureUrl = pictureUrl;
        if (locale != null && !locale.isBlank()) this.locale = locale;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getProvider() { return provider; }
    public String getProviderSubject() { return providerSubject; }
    public String getEmailAtLink() { return emailAtLink; }
    public boolean isEmailVerified() { return emailVerified; }
    public String getDisplayName() { return displayName; }
    public String getPictureUrl() { return pictureUrl; }
    public String getLocale() { return locale; }
    public Instant getLinkedAt() { return linkedAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Instant getCreatedAt() { return createdAt; }
}
