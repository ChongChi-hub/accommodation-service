package vn.cozynest.identity.application.port;

import vn.cozynest.identity.domain.model.User;

public interface TokenProviderPort {
    String generateAccessToken(User user);
    String generateRefreshToken();
    String hashToken(String rawToken);
    boolean validateToken(String token);
    String extractUserId(String token);
}

