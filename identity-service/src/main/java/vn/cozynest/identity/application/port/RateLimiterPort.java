package vn.cozynest.identity.application.port;

public interface RateLimiterPort {
    void checkForgotPasswordRateLimit(String email, String ipAddress);

    void checkPasswordChangeAllowed(String userId, String ipAddress);

    void recordFailedPasswordChange(String userId, String ipAddress);

    void clearPasswordChangeAttempts(String userId, String ipAddress);
}

