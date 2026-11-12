package vn.cozynest.identity.domain.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class UserTwoFactor {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 15;

    private final String userId;
    private String totpSecret;
    private boolean enabled;
    private int failedAttempts;
    private Instant lockedUntil;
    private String backupCodesJson;
    private final Instant createdAt;
    private Instant updatedAt;

    public UserTwoFactor(String userId, String totpSecret, boolean enabled,
                         String backupCodesJson, Instant createdAt, Instant updatedAt) {
        this(userId, totpSecret, enabled, 0, null, backupCodesJson, createdAt, updatedAt);
    }

    public UserTwoFactor(String userId, String totpSecret, boolean enabled,
                         int failedAttempts, Instant lockedUntil,
                         String backupCodesJson, Instant createdAt, Instant updatedAt) {
        this.userId = userId;
        this.totpSecret = totpSecret;
        this.enabled = enabled;
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
        this.backupCodesJson = backupCodesJson;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static UserTwoFactor createPending(String userId, String totpSecret, String backupCodesJson) {
        return new UserTwoFactor(userId, totpSecret, false, 0, null, backupCodesJson, Instant.now(), Instant.now());
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    public void recordFailedAttempt(Instant now) {
        this.failedAttempts++;
        if (this.failedAttempts >= MAX_FAILED_ATTEMPTS) {
            this.lockedUntil = now.plus(LOCK_MINUTES, ChronoUnit.MINUTES);
        }
        this.updatedAt = now;
    }

    public void resetAttempts() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    public void replaceBackupCodesJson(String updatedJson) {
        this.backupCodesJson = updatedJson;
        this.updatedAt = Instant.now();
    }

    public void enable() {
        this.enabled = true;
        this.failedAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    public void disable() {
        this.enabled = false;
        this.failedAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    public void updateSecret(String newSecret, String newBackupCodesJson) {
        this.totpSecret = newSecret;
        this.backupCodesJson = newBackupCodesJson;
        this.enabled = false;
        this.failedAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    public String getUserId() { return userId; }
    public String getTotpSecret() { return totpSecret; }
    public boolean isEnabled() { return enabled; }
    public int getFailedAttempts() { return failedAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public String getBackupCodesJson() { return backupCodesJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
