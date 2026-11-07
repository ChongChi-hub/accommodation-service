package vn.toam.identity.application.port;

public interface RateLimiterPort {
    void checkForgotPasswordRateLimit(String email, String ipAddress);
}

