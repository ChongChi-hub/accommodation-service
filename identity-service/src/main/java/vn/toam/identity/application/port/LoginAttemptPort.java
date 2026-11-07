package vn.toam.identity.application.port;

public interface LoginAttemptPort {
    void recordAttempt(String email, String ipAddress, boolean success);
    boolean isBlocked(String email);
}
