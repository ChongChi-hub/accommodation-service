package vn.cozynest.identity.domain.model;

import java.time.Instant;

public class UserTwoFactor {
    private final String userId;
    private String totpSecret;
    private boolean enabled;
    private String backupCodesJson;
    private final Instant createdAt;
    private Instant updatedAt;

    public UserTwoFactor(String userId, String totpSecret, boolean enabled,
                         String backupCodesJson, Instant createdAt, Instant updatedAt) {
        this.userId = userId;
        this.totpSecret = totpSecret;
        this.enabled = enabled;
        this.backupCodesJson = backupCodesJson;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static UserTwoFactor createPending(String userId, String totpSecret, String backupCodesJson) {
        return new UserTwoFactor(userId, totpSecret, false, backupCodesJson, Instant.now(), Instant.now());
    }

    public void enable() {
        this.enabled = true;
        this.updatedAt = Instant.now();
    }

    public void disable() {
        this.enabled = false;
        this.updatedAt = Instant.now();
    }

    public void updateSecret(String newSecret, String newBackupCodesJson) {
        this.totpSecret = newSecret;
        this.backupCodesJson = newBackupCodesJson;
        this.enabled = false;
        this.updatedAt = Instant.now();
    }

    public String getUserId() { return userId; }
    public String getTotpSecret() { return totpSecret; }
    public boolean isEnabled() { return enabled; }
    public String getBackupCodesJson() { return backupCodesJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

