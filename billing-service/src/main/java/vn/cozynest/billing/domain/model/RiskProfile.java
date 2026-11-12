package vn.cozynest.billing.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

public class RiskProfile {
    private static final int MAX_DAILY_VELOCITY = 3;
    private static final long BANK_CHANGE_COOLDOWN_HOURS = 24;

    private final String userId;
    private long dailyLimit;
    private long dailyWithdrawnAmount;
    private LocalDate lastWithdrawalDate;
    private int velocityCount;
    private boolean frozen;
    private String freezeReason;
    private Instant bankChangedAt;
    private boolean blacklisted;
    private String blacklistReason;
    private Instant updatedAt;

    public RiskProfile(String userId, long dailyLimit, long dailyWithdrawnAmount,
                       LocalDate lastWithdrawalDate, int velocityCount,
                       boolean frozen, String freezeReason, Instant updatedAt) {
        this(userId, dailyLimit, dailyWithdrawnAmount, lastWithdrawalDate, velocityCount,
                frozen, freezeReason, null, false, null, updatedAt);
    }

    public RiskProfile(String userId, long dailyLimit, long dailyWithdrawnAmount,
                       LocalDate lastWithdrawalDate, int velocityCount,
                       boolean frozen, String freezeReason, Instant bankChangedAt,
                       boolean blacklisted, String blacklistReason, Instant updatedAt) {
        this.userId = userId;
        this.dailyLimit = dailyLimit > 0 ? dailyLimit : 20_000_000L;
        this.dailyWithdrawnAmount = dailyWithdrawnAmount;
        this.lastWithdrawalDate = lastWithdrawalDate;
        this.velocityCount = velocityCount;
        this.frozen = frozen;
        this.freezeReason = freezeReason;
        this.bankChangedAt = bankChangedAt;
        this.blacklisted = blacklisted;
        this.blacklistReason = blacklistReason;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static RiskProfile createDefault(String userId) {
        return new RiskProfile(userId, 20_000_000L, 0L, null, 0, false, null, null, false, null, Instant.now());
    }

    public void validateWithdrawal(long amount, LocalDate today) {
        validateWithdrawal(amount, today, Instant.now());
    }

    public void validateWithdrawal(long amount, LocalDate today, Instant now) {
        if (this.blacklisted) {
            throw new IllegalStateException("Tai khoan nam trong danh sach cam giao dich: " + (blacklistReason != null ? blacklistReason : "BLACKLIST"));
        }
        if (this.frozen) {
            throw new IllegalStateException("Tai khoan dang bi dong bang: " + (freezeReason != null ? freezeReason : "Vi pham quy tac an toan"));
        }
        if (this.bankChangedAt != null && Duration.between(this.bankChangedAt, now).toHours() < BANK_CHANGE_COOLDOWN_HOURS) {
            throw new IllegalStateException("Tai khoan dang trong thoi gian tam khoa 24 gio sau khi doi thong tin ngan hang");
        }
        int currentVelocity = today.equals(lastWithdrawalDate) ? this.velocityCount : 0;
        if (currentVelocity >= MAX_DAILY_VELOCITY) {
            throw new IllegalStateException("Vuot qua tan suat rut tien cho phep trong ngay (toi da " + MAX_DAILY_VELOCITY + " lan)");
        }
        long currentDayWithdrawn = today.equals(lastWithdrawalDate) ? this.dailyWithdrawnAmount : 0L;
        if (currentDayWithdrawn + amount > this.dailyLimit) {
            throw new IllegalStateException("So tien rut vuot qua han muc ngay (" + dailyLimit + " VND)");
        }
    }

    public void recordWithdrawal(long amount, LocalDate today) {
        if (today.equals(lastWithdrawalDate)) {
            this.dailyWithdrawnAmount += amount;
            this.velocityCount++;
        } else {
            this.dailyWithdrawnAmount = amount;
            this.lastWithdrawalDate = today;
            this.velocityCount = 1;
        }
        this.updatedAt = Instant.now();
    }

    public void markBankChanged(Instant at) {
        this.bankChangedAt = at != null ? at : Instant.now();
        this.updatedAt = Instant.now();
    }

    public void freeze(String reason) {
        this.frozen = true;
        this.freezeReason = reason;
        this.updatedAt = Instant.now();
    }

    public void unfreeze() {
        this.frozen = false;
        this.freezeReason = null;
        this.updatedAt = Instant.now();
    }

    public void blacklist(String reason) {
        this.blacklisted = true;
        this.blacklistReason = reason;
        this.frozen = true;
        this.freezeReason = reason;
        this.updatedAt = Instant.now();
    }

    public String getUserId() { return userId; }
    public long getDailyLimit() { return dailyLimit; }
    public long getDailyWithdrawnAmount() { return dailyWithdrawnAmount; }
    public LocalDate getLastWithdrawalDate() { return lastWithdrawalDate; }
    public int getVelocityCount() { return velocityCount; }
    public boolean isFrozen() { return frozen; }
    public String getFreezeReason() { return freezeReason; }
    public Instant getBankChangedAt() { return bankChangedAt; }
    public boolean isBlacklisted() { return blacklisted; }
    public String getBlacklistReason() { return blacklistReason; }
    public Instant getUpdatedAt() { return updatedAt; }
}
