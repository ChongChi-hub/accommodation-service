package vn.cozynest.identity.application.port;

public interface LoginAttemptPort {
    void recordAttempt(String email, String ipAddress, boolean success);
    boolean isBlocked(String email);
    boolean isBlocked(String email, String ipAddress);
}

